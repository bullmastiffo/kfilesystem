package com.mvg.virtualfs.core

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.mvg.virtualfs.storage.BlockGroup
import com.mvg.virtualfs.storage.INode
import com.mvg.virtualfs.storage.serialization.serializeToChannel
import com.mvg.virtualfs.storage.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Basic implementation of AllocatingBlockGroup
 * @property groupId Int
 * @property blockGroup BlockGroup
 * @property freeBlocksCounter AtomicInteger
 * @property freeInodesCounter AtomicInteger
 * @property blocksLock ReentrantLock
 * @property inodesLock ReentrantLock
 * @property id Int
 * @property freeBlocks Int
 * @property freeInodes Int
 * @constructor
 */
class ActiveAllocatingBlockGroup(private val groupId: Int, private val blockGroup: BlockGroup): AllocatingBlockGroup {

    private val freeBlocksCounter = AtomicInteger(blockGroup.freeBlocksCount)
    private val freeInodesCounter = AtomicInteger(blockGroup.freeInodesCount)

    private val blocksLock = ReentrantLock()
    private val inodesLock = ReentrantLock()

    override val id: Int
        get() = groupId
    override val freeBlocks: Int
        get() = freeBlocksCounter.get()

    override fun reserveBlockAndGetOffset(serializer: FileSystemSerializer): Either<CoreFileSystemError, Long> {
        if(freeBlocksCounter.getAndDecrement() <= 0){
            freeBlocksCounter.incrementAndGet()
            return CoreFileSystemError.VolumeIsFullError.left()
        }
        var blockIndex: Int
        blocksLock.withLock {
            blockIndex = blockGroup.blockBitMap.nextClearBit()
            blockGroup.blockBitMap.flipBit(blockIndex)
            blockGroup.freeBlocksCount--
            val (byte, index) = blockGroup.blockBitMap.getHoldingByteAndIndex(blockIndex)
            val offset = blockGroup.bitmapBlocksOffset + index
            serializer.runSerializationAction {
                it.position(blockGroup.freeBlocksCountOffset).writeInt(blockGroup.freeBlocksCount)
                it.position(offset).writeByte(byte)
            }
        }
        return (blockGroup.dataBlocksOffset + blockIndex * blockGroup.blockSize).right()
    }

    override fun markBlockFree(serializer: FileSystemSerializer, offset: Long): Either<CoreFileSystemError, Unit> {
        val blockIndex = ((offset - blockGroup.dataBlocksOffset) / blockGroup.blockSize).toInt()
        blocksLock.withLock {
            blockGroup.blockBitMap.flipBit(blockIndex)
            blockGroup.freeBlocksCount++
            val (byte, index) = blockGroup.blockBitMap.getHoldingByteAndIndex(blockIndex)
            val byteOffset = blockGroup.bitmapBlocksOffset + index
            serializer.runSerializationAction {
                it.position(byteOffset).writeByte(byte)
                it.position(blockGroup.freeBlocksCountOffset).writeInt(blockGroup.freeBlocksCount)
            }
        }
        freeBlocksCounter.incrementAndGet()
        return Unit.right()
    }

    override val freeInodes: Int
        get() = freeInodesCounter.get()

    override fun acquireInode(inodeId: Int): Either<CoreFileSystemError, INodeAccessor> {
        val inodeIndex = inodeId - blockGroup.inodes[0].id
        if (inodeIndex !in blockGroup.inodes.indices)
        {
            CoreFileSystemError.FileSystemCorruptedError.left()
        }
        val inode = blockGroup.inodes[inodeIndex]
        return ActiveINodeAccessor(
                blockGroup.blockSize,
                inode,
                blockGroup.inodesOffset + inodeIndex * INode.sizeInBytes(),
                ViFsAttributeSet(inode)).right()
    }

    override fun getInode(inodeId: Int): Either<CoreFileSystemError, ItemDescriptor> {
        val inodeIndex = inodeId - blockGroup.inodes[0].id
        if (inodeIndex !in blockGroup.inodes.indices)
        {
            CoreFileSystemError.FileSystemCorruptedError.left()
        }
        val inode = blockGroup.inodes[inodeIndex]
        val nodeType = when(inode.type){
            NodeType.None -> return CoreFileSystemError.FileSystemCorruptedError.left()
            NodeType.Folder -> ItemType.Folder
            NodeType.File -> ItemType.File
        }
        return ItemDescriptor(inodeId, nodeType, ViFsAttributeSet(inode))
                .right()
    }

    override fun reserveInode(coreFileSystem: CoreFileSystem): Either<CoreFileSystemError, INodeAccessor> {
        if(freeInodesCounter.getAndDecrement() <= 0){
            freeInodesCounter.incrementAndGet()
            return CoreFileSystemError.CantCreateMoreItemsError.left()
        }
        var inodeIndex: Int
        inodesLock.withLock {
            inodeIndex = blockGroup.inodesBitMap.nextClearBit()
            blockGroup.inodesBitMap.flipBit(inodeIndex)
            val (byte, index) = blockGroup.inodesBitMap.getHoldingByteAndIndex(inodeIndex)
            val offset = blockGroup.bitmapInodesOffset + index
            blockGroup.freeInodesCount--
            coreFileSystem.runSerializationAction {
                it.position(blockGroup.freeInodesCountOffset).writeInt(blockGroup.freeInodesCount)
                it.position(offset).writeByte(byte)
            }
        }
        val now = coreFileSystem.time.now()
        val inode = blockGroup.inodes[inodeIndex]
        inode.created = now
        inode.lastModified = now
        return ActiveINodeAccessor(
                blockGroup.blockSize,
                inode,
                blockGroup.inodesOffset + inodeIndex * INode.sizeInBytes(),
                ViFsAttributeSet(inode)).right()
    }

    override fun markInodeFree(coreFileSystem: CoreFileSystem, inode: INodeAccessor): Either<CoreFileSystemError, Unit> {
        val inodeIndex = inode.id - blockGroup.inodes[0].id
        if (inodeIndex >= blockGroup.inodes.size)
        {
            CoreFileSystemError.FileSystemCorruptedError.left()
        }
        inodesLock.withLock {
            blockGroup.inodesBitMap.flipBit(inodeIndex)
            blockGroup.freeInodesCount++
            val (byte, index) = blockGroup.inodesBitMap.getHoldingByteAndIndex(inodeIndex)
            val offset = blockGroup.bitmapInodesOffset + index
            blockGroup.inodes[inodeIndex].type = NodeType.None
            blockGroup.inodes[inodeIndex].created = null
            blockGroup.inodes[inodeIndex].lastModified = null
            coreFileSystem.runSerializationAction {
                it.position(offset).writeByte(byte)
                it.position(blockGroup.freeInodesCountOffset).writeInt(blockGroup.freeInodesCount)
                it.position(blockGroup.inodesOffset + inodeIndex * INode.sizeInBytes())
                serializeToChannel(it, blockGroup.inodes[inodeIndex])
            }
        }
        freeInodesCounter.incrementAndGet()
        return Unit.right()
    }
}