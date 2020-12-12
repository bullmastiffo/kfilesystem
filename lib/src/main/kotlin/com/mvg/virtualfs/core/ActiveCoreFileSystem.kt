package com.mvg.virtualfs.core

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.mvg.virtualfs.FileSystemInfo
import com.mvg.virtualfs.Time
import com.mvg.virtualfs.storage.FIRST_BLOCK_OFFSET
import com.mvg.virtualfs.storage.FolderEntry
import com.mvg.virtualfs.storage.serialization.serializeToChannel
import java.nio.channels.SeekableByteChannel
import java.util.concurrent.locks.ReentrantLock

class ActiveCoreFileSystem(private val superGroup: SuperGroupAccessor,
                           private val blockGroups: Array<AllocatingBlockGroup>,
                           private val serializer: FileSystemSerializer,
                           private val handlersPool: ConcurrentPool<Int, ItemHandler>,
                           override val time: Time) : CoreFileSystem {

    private val blockGroupSize = superGroup.blockSize * superGroup.blockPerGroup

    fun getOrCreateRootFolder() : Either<CoreFileSystemError, FolderHandler> {
        return getInodeItemDescriptor(0).fold(
                { createRootFolder() },
                { initializeItemHandler(NamedItemDescriptor("", it)).flatMap { h ->
                    (h as FolderHandler).right() } })
    }

    private fun createRootFolder() : Either<CoreFileSystemError, FolderHandler> {
        return createItem("", ItemType.Folder,
                {
                    if(it.id != 0){
                        CoreFileSystemError.FileSystemCorruptedError.left()
                    }else {
                        initFolder(it)
                    }
                },
                { inode, descriptor -> ActiveFolderHandler(inode, this, descriptor) })
    }

    private fun createFolder(name: String) : Either<CoreFileSystemError, FolderHandler>{
        return createItem(name, ItemType.Folder,
                { initFolder(it) },
                { inode, descriptor -> ActiveFolderHandler(inode, this, descriptor) })
    }

    private fun createFile(name: String) : Either<CoreFileSystemError, FileHandler>{
        return createItem(name, ItemType.File,
                { it.type = NodeType.File; Unit.right() },
                { inode, descriptor -> ActiveFileHandler(inode, this, descriptor) })
    }

    private fun <T: ItemHandler> createItem(
            name: String,
            itemType: ItemType,
            initAction: (INodeAccessor) -> Either<CoreFileSystemError, Unit>,
            factoryMethod: (INodeAccessor, descriptor: NamedItemDescriptor) -> T)
                : Either<CoreFileSystemError, T>{
        //init
        val inode = when (val r = reserveInode()){
            is Either.Left -> return r
            is Either.Right -> r.b
        }
        val blockOffset = when(val r = reserveBlockAndGetOffset(inode.id)){
            is Either.Left -> { freeInode(inode); return r}
            is Either.Right -> r.b
        }
        when(val r = inode.addInitialDataBlock(blockOffset)){
            is Either.Left -> return r
        }
        when(val r = initAction(inode)){
            is Either.Left -> {
                freeInode(inode)
                freeBlock(blockOffset)
                return r
            }
        }
        val handler = factoryMethod(inode, NamedItemDescriptor(inode.id, itemType, inode.attributeSet, name))
        return (handlersPool.getOrPut(inode.id) { handler }).right()
    }

    private fun initFolder(inode: INodeAccessor) : Either<CoreFileSystemError, Unit> {
        inode.type = NodeType.Folder
        val terminatingEntry = FolderEntry.TerminatingEntry
        val l = ReentrantLock()
        l.lock()
        val ch = inode.getSeekableByteChannel(this, l)
        serializeToChannel(ch, terminatingEntry)
        serializer.runSerializationAction {
            inode.serialize(it)
        }
        ch.close()
        return Unit.right()
    }

    override val fileSystemInfo: FileSystemInfo
        get() = FileSystemInfo(
            superGroup.totalBlocks.toLong() * superGroup.blockSize,
            superGroup.freeBlockCount.toLong() * superGroup.blockSize,
                superGroup.freeInodeCount,
                superGroup.freeBlockCount,
                superGroup.blockSize)

    override fun reserveBlockAndGetOffset(inodeId: Int): Either<CoreFileSystemError, Long> {
        return when(val r = reserveAndGetItem(inodeId, { it.reserveBlockAndGetOffset(serializer) }, CoreFileSystemError.VolumeIsFullError))
        {
            is Either.Left -> r
            is Either.Right -> {
                superGroup.decrementFreeBlockCount()
                r
            }
        }
    }

    override fun freeBlock(offset: Long): Either<CoreFileSystemError, Unit> {
        val blockGroupIndex = getDataBlockGroupIndex(offset)
        when (val r = blockGroups[blockGroupIndex].markBlockFree(serializer, offset))
        {
            is Either.Left -> return r
        }
        superGroup.incrementFreeBlockCount()
        return Either.Right(Unit)
    }

    override fun initializeItemHandler(entry: NamedItemDescriptor): Either<CoreFileSystemError, ItemHandler> {
        return getInodeAccessor(entry.nodeId).flatMap { acc ->
                handlersPool.getOrPut(entry.nodeId) {
                    when (entry.type) {
                        ItemType.Folder -> ActiveFolderHandler(acc, this, entry)
                        ItemType.File -> ActiveFileHandler(acc, this, entry)
                    }
                }.right()
            }
        }

    override fun getInodeItemDescriptor(inodeId: Int): Either<CoreFileSystemError, ItemDescriptor> {
        val blockGroupIndex = getInodeBlockGroupIndex(inodeId)
        val bg = blockGroups[blockGroupIndex]
        return bg.getInode(inodeId)
    }

    override fun createItem(type: ItemType, name: String): Either<CoreFileSystemError, ItemHandler> {
        return when(type){
            ItemType.Folder -> createFolder(name)
            ItemType.File -> createFile(name)
        }
    }

    override fun deleteItem(inodeAccessor: INodeAccessor): Either<CoreFileSystemError, Unit> {
        return inodeAccessor.removeInitialDataBlock().flatMap { offset ->
            handlersPool.removeItem(inodeAccessor.id)
            freeBlock(offset)
            freeInode(inodeAccessor)
        }
    }

    private fun reserveInode(): Either<CoreFileSystemError, INodeAccessor> {
        return when(val r = reserveAndGetItem(0, { it.reserveInode(this) }, CoreFileSystemError.CantCreateMoreItemsError))
        {
            is Either.Left -> r
            is Either.Right -> {
                superGroup.decrementFreeInodeCount()
                r
            }
        }
    }

    private fun <T> reserveAndGetItem(
            inodeId: Int,
            blockGetter: (AllocatingBlockGroup) -> Either<CoreFileSystemError, T>,
            error: CoreFileSystemError): Either<CoreFileSystemError, T> {
        val firstIndex = getInodeBlockGroupIndex(inodeId)
        var blockGroup = blockGroups[firstIndex]
        var index = firstIndex
        var receivedItem: T? = null
        do {
            when(val r = blockGetter(blockGroup)){
                is Either.Right -> { receivedItem = r.b; break; }
                is Either.Left -> if (r.a != error) {return r}
            }
            index++
            if(index == blockGroups.size) {
                index = 0
            }
            blockGroup = blockGroups[index]
        } while (index != firstIndex)

        if(receivedItem == null)
            return Either.Left(error)
        return Either.Right(receivedItem)
    }

    private fun freeInode(nodeAccessor: INodeAccessor): Either<CoreFileSystemError, Unit> {
        val blockGroupIndex = getInodeBlockGroupIndex(nodeAccessor.id)
        when (val r = blockGroups[blockGroupIndex].markInodeFree(this, nodeAccessor))
        {
            is Either.Left -> return r
        }
        superGroup.incrementFreeInodeCount()
        return Either.Right(Unit)
    }

    private fun getInodeAccessor(nodeId: Int) : Either<CoreFileSystemError, INodeAccessor>{
        val blockGroupIndex = getInodeBlockGroupIndex(nodeId)
        return blockGroups[blockGroupIndex].acquireInode(nodeId)
    }

    override fun runSerializationAction(action: (SeekableByteChannel) -> Unit) {
        serializer.runSerializationAction(action)
    }

    override fun close() {
        // TODO release all handlers forcefully
        serializer.runSerializationAction { superGroup.serialize(it) }
        serializer.close()
    }

    private fun getDataBlockGroupIndex(offset: Long) = ((offset - FIRST_BLOCK_OFFSET) / blockGroupSize).toInt()

    private fun getInodeBlockGroupIndex(inodeId: Int) = inodeId / superGroup.inodesPerGroup
}