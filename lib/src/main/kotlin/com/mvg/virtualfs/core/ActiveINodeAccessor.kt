package com.mvg.virtualfs.core

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.mvg.virtualfs.storage.INode
import com.mvg.virtualfs.storage.INode.Companion.OFFSETS_SIZE
import com.mvg.virtualfs.storage.ceilDivide
import com.mvg.virtualfs.storage.serialization.serializeToChannel
import com.mvg.virtualfs.storage.writeLong
import java.io.IOException
import java.lang.IllegalArgumentException
import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.Lock
import kotlin.math.*

/**
 * INodeAccessor implementation that manages data blocks and provides access to it's content as a channel.
 * @property blockSize Int
 * @property inode INode
 * @property offsetInUnderlyingStream Long
 * @property lock Lock
 * @property viFsAttributeSet ViFsAttributeSet
 * @property isOpen AtomicBoolean
 * @property dataBlocks ArrayList<Long>
 * @property doubleIndirectBlocks ArrayList<Long>
 * @property maxBlocksCount Int
 * @property id Int
 * @property type NodeType
 * @property attributeSet AttributeSet
 * @property size Long
 * @constructor
 */
class ActiveINodeAccessor(
        private val blockSize: Int,
        private val inode: INode,
        private val offsetInUnderlyingStream: Long,
        private val lock: Lock,
        private val viFsAttributeSet: ViFsAttributeSet) : INodeAccessor {
    private val isOpen = AtomicBoolean(true)
    private val dataBlocks = ArrayList<Long>()
    private val doubleIndirectBlocks = ArrayList<Long>()
    private val maxBlocksCount = INDIRECT_INDEX + blockSize / Long.SIZE_BYTES + (blockSize / Long.SIZE_BYTES) * (blockSize / Long.SIZE_BYTES)

    override val id: Int
        get() = inode.id
    override var type: NodeType
        get() = inode.type
        set(value) { inode.type = value }
    override val attributeSet: AttributeSet
        get() = viFsAttributeSet
    override val size: Long
        get() = inode.dataSize

    override fun addInitialDataBlock(offset: Long): Either<CoreFileSystemError, Unit> {
            if(inode.blockOffsets[0] != 0L)
            {
                return CoreFileSystemError.FileSystemCorruptedError.left()
            }
        inode.blockOffsets[0] = offset
        return Unit.right()
    }

    override fun removeInitialDataBlock(): Either<CoreFileSystemError, Long> {
        if(inode.blockOffsets[0] == 0L)
        {
            return CoreFileSystemError.FileSystemCorruptedError.left()
        }
        val offset = inode.blockOffsets[0]
        inode.blockOffsets[0] = 0
        return offset.right()
    }

    override fun serialize(channel: SeekableByteChannel) {
        channel.position(offsetInUnderlyingStream)
        serializeToChannel(channel, inode)
    }

    override fun getSeekableByteChannel(coreFileSystem: CoreFileSystem): SeekableByteChannel {
        val totalFileBlocks = ceilDivide(inode.dataSize.toInt(), blockSize)

        if(totalFileBlocks > INDIRECT_INDEX){
            val indirectPosition = inode.blockOffsets[INDIRECT_INDEX]
            var blockCounter = INDIRECT_INDEX

            fun readIndirectBlock(position: Long) {
                var ob = ByteBuffer.allocate(blockSize)
                coreFileSystem.runSerializationAction {
                    it.position(position)
                            .read(ob)
                }
                ob.flip()
                while (ob.hasRemaining() && blockCounter < totalFileBlocks) {
                    dataBlocks.add(ob.getLong())
                    blockCounter++
                }
            }
            readIndirectBlock(indirectPosition)
            if(blockCounter < totalFileBlocks){
                val doubleIndirectPosition = inode.blockOffsets[DOUBLE_INDIRECT_INDEX]
                var ob = ByteBuffer.allocate(blockSize)
                coreFileSystem.runSerializationAction {
                    it.position(doubleIndirectPosition)
                            .read(ob)
                }
                ob.flip()
                while (ob.hasRemaining() && blockCounter < totalFileBlocks) {
                    val indirectIndex = ob.getLong()
                    if(indirectIndex == 0L)
                        break
                    readIndirectBlock(indirectIndex)
                }
            }
        }

        return SeekableByteChannelOnTopOfBlocks(coreFileSystem)
    }

    override fun close() {
        if(isOpen.getAndSet(false)) {
            lock.unlock()
        }
    }

    fun getDataBlockAtOffset(
            coreFileSystem: CoreFileSystem,
            infileOffset: Long,
            forWrite: Boolean = false) : Pair<Long, Int> {

        if(infileOffset >= inode.dataSize && !forWrite){
            return Pair(inode.dataSize, 0)
        }

        val totalFileBlocks = max(ceilDivide(inode.dataSize.toInt(), blockSize), 1)
        val blockIndex = (infileOffset / blockSize).toInt()
        val offsetInBlock = (infileOffset % blockSize).toInt()

        if(blockIndex >= maxBlocksCount){
            TODO("Return either error")
        }

        if (blockIndex >= totalFileBlocks && forWrite){
            val offsetsInBlock = blockSize / Long.SIZE_BYTES
            val lastIndirectIndex = INDIRECT_INDEX + offsetsInBlock - 1
            var block = totalFileBlocks
            do {
                val newOffset = reserveDataBlock(coreFileSystem)

                fun writeOffsetToIndirectBlock(position: Long, offset: Long, writeStop: Boolean)
                {
                    coreFileSystem.runSerializationAction {
                        it.position(position)
                                .writeLong(offset)
                        if(writeStop)
                            it.writeLong(0L)
                    }
                }

                when {
                    block < INDIRECT_INDEX -> {
                        inode.blockOffsets[block] = newOffset
                    }
                    block in INDIRECT_INDEX..lastIndirectIndex -> {
                        if(block == INDIRECT_INDEX) {
                            inode.blockOffsets[block] = reserveDataBlock(coreFileSystem)
                        }
                        val offsetInIndirectBlock = inode.blockOffsets[INDIRECT_INDEX] + (block - INDIRECT_INDEX) * Long.SIZE_BYTES
                        writeOffsetToIndirectBlock(offsetInIndirectBlock, newOffset, block < lastIndirectIndex)
                        dataBlocks.add(newOffset)
                    }
                    else -> {
                        // double indirect handling
                        if(block == lastIndirectIndex + 1) {
                            inode.blockOffsets[DOUBLE_INDIRECT_INDEX] = reserveDataBlock(coreFileSystem)
                        }
                        val doubleIndex = block - lastIndirectIndex - 1
                        val indexOfDoubleBlock = doubleIndex / offsetsInBlock
                        val indexInDoubleBlock = doubleIndex % offsetsInBlock
                        if (indexInDoubleBlock == 0){
                            val dblock = reserveDataBlock(coreFileSystem)
                            val offsetInIndirectBlock = inode.blockOffsets[DOUBLE_INDIRECT_INDEX] + indexOfDoubleBlock * Long.SIZE_BYTES
                            writeOffsetToIndirectBlock(offsetInIndirectBlock, dblock, indexOfDoubleBlock < offsetsInBlock - 1)
                            doubleIndirectBlocks.add(dblock)
                        }
                        val offsetInIndirectBlock = doubleIndirectBlocks[indexOfDoubleBlock] + indexInDoubleBlock * Long.SIZE_BYTES
                        writeOffsetToIndirectBlock(offsetInIndirectBlock, newOffset, indexInDoubleBlock < offsetsInBlock - 1)
                        dataBlocks.add(newOffset)
                    }
                }

                block++
            } while (block < blockIndex)
        }

        fun getRemainder(): Int {
            if(forWrite || blockIndex < totalFileBlocks - 1)
            {
                return blockSize - offsetInBlock
            }
            val dataInLastBlock  = (inode.dataSize % blockSize).toInt()
            return dataInLastBlock - offsetInBlock
        }

        if(blockIndex < INDIRECT_INDEX){
            return Pair(inode.blockOffsets[blockIndex], getRemainder())
        }
        val indexInTail = blockIndex - INDIRECT_INDEX
        return Pair(dataBlocks[indexInTail], getRemainder())
    }

    private fun truncateToSize(coreFileSystem: CoreFileSystem, newSize: Long) {
        val totalFileBlocks = max(ceilDivide(inode.dataSize.toInt(), blockSize), 1)
        val newLastBlockIndex = max(ceilDivide(newSize.toInt(), blockSize), 1) - 1
        var block = totalFileBlocks - 1
        val offsetsInBlock = blockSize / Long.SIZE_BYTES
        val lastIndirectIndex = INDIRECT_INDEX + offsetsInBlock - 1

        while (block > newLastBlockIndex) {
            fun writeZeroToIndirectBlock(position: Long)
            {
                coreFileSystem.runSerializationAction {
                    it.position(position).writeLong(0L)
                }
            }

            var blockToRelease = 0L
            when {
                block < INDIRECT_INDEX -> {
                    blockToRelease = inode.blockOffsets[block]
                    inode.blockOffsets[block] = 0L
                }
                block in INDIRECT_INDEX..lastIndirectIndex -> {
                    blockToRelease = dataBlocks.removeAt(dataBlocks.size - 1)

                    val offsetInIndirectBlock = inode.blockOffsets[INDIRECT_INDEX] + (block - INDIRECT_INDEX) * Long.SIZE_BYTES
                    writeZeroToIndirectBlock(offsetInIndirectBlock)

                    if(block == INDIRECT_INDEX) {
                        coreFileSystem.freeBlock(inode.blockOffsets[block])
                    }
                }
                else -> {
                    // double indirect handling
                    blockToRelease = dataBlocks.removeAt(dataBlocks.size - 1)

                    val doubleIndex = block - lastIndirectIndex - 1
                    val indexOfDoubleBlock = doubleIndex / offsetsInBlock
                    val indexInDoubleBlock = doubleIndex % offsetsInBlock
                    val offsetInDoubleIndirectBlock = doubleIndirectBlocks[indexOfDoubleBlock] + indexInDoubleBlock * Long.SIZE_BYTES
                    writeZeroToIndirectBlock(offsetInDoubleIndirectBlock)

                    if (indexInDoubleBlock == 0){
                        val dblock = doubleIndirectBlocks.removeAt(doubleIndirectBlocks.size - 1)
                        coreFileSystem.freeBlock(dblock)
                        val offsetInIndirectBlock = inode.blockOffsets[DOUBLE_INDIRECT_INDEX] + indexOfDoubleBlock * Long.SIZE_BYTES
                        writeZeroToIndirectBlock(offsetInIndirectBlock)
                    }

                    if(block == lastIndirectIndex + 1) {
                        coreFileSystem.freeBlock(inode.blockOffsets[DOUBLE_INDIRECT_INDEX])
                        inode.blockOffsets[DOUBLE_INDIRECT_INDEX] = 0L
                    }
                }
            }

            coreFileSystem.freeBlock(blockToRelease)
            block--
        }
        inode.dataSize = newSize
        coreFileSystem.runSerializationAction {
            serialize(it)
        }
    }

    private fun reserveDataBlock(coreFileSystem: CoreFileSystem): Long {
        val of = when (val r = coreFileSystem.reserveBlockAndGetOffset(inode.id)) {
            is Either.Left -> TODO()
            is Either.Right -> r.b
        }
        return of
    }

    /**
     * Implementation of @see SeekableByteChannel based on virtual fs inode
     * @property coreFileSystem CoreFileSystem
     * @constructor
     */
    inner class SeekableByteChannelOnTopOfBlocks(
            private val coreFileSystem: CoreFileSystem) : SeekableByteChannel
    {
        private var streamPosition = 0L

        override fun close() {
            this@ActiveINodeAccessor.close()
        }

        override fun isOpen(): Boolean = this@ActiveINodeAccessor.isOpen.get()

        override fun read(buffer: ByteBuffer?): Int {
            if(buffer == null)
                throw IllegalArgumentException("buffer must not be null")

            var read = 0
            while(buffer.hasRemaining() && streamPosition < size())
            {
                val (startBlockOffset, size) = this@ActiveINodeAccessor.getDataBlockAtOffset(coreFileSystem, streamPosition)
                val toRead = min(size, buffer.remaining())
                val readBuffer = ByteBuffer.allocate(toRead)
                val offset = startBlockOffset + streamPosition
                coreFileSystem.runSerializationAction {
                    it.position(offset).read(readBuffer)
                }
                buffer.put(readBuffer.flip())
                streamPosition+=toRead
                read+=toRead
            }
            return read
        }

        override fun write(buffer: ByteBuffer?): Int {
            if(buffer == null)
                throw IllegalArgumentException("buffer must not be null")
            var written = 0
            while(buffer.hasRemaining())
            {
                val (startBlockoffset, size) = this@ActiveINodeAccessor.getDataBlockAtOffset(coreFileSystem, streamPosition, true)
                val toWrite = min(size, buffer.remaining())
                val offset = startBlockoffset + streamPosition
                coreFileSystem.runSerializationAction {
                    it.position(offset).write( buffer.slice(buffer.position(), toWrite) )
                    streamPosition+=toWrite
                    this@ActiveINodeAccessor.inode.lastModified = coreFileSystem.time.now()
                    if(streamPosition > size()){
                        this@ActiveINodeAccessor.inode.dataSize = streamPosition
                    }
                    this@ActiveINodeAccessor.serialize(it)
                }
                buffer.position(buffer.position() + toWrite)
                written+=toWrite
            }
            return written
        }

        override fun position(): Long = streamPosition

        override fun position(newPosition: Long): SeekableByteChannel {
            streamPosition = newPosition
            return this
        }

        override fun size(): Long = this@ActiveINodeAccessor.inode.dataSize

        override fun truncate(newSize: Long): SeekableByteChannel {
            if (newSize < 0)
                throw IOException("newSize must be not less then 0")
            if(newSize >= size())
                return this
            if(streamPosition > newSize)
                streamPosition = newSize

            this@ActiveINodeAccessor.truncateToSize(coreFileSystem, newSize)

            return this
        }

    }

    protected fun finalize() {
        close()
    }

    companion object {
        const val INDIRECT_INDEX = OFFSETS_SIZE - 2
        const val DOUBLE_INDIRECT_INDEX = OFFSETS_SIZE - 1
    }
}