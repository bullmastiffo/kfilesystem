package com.mvg.virtualfs.core

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.mvg.virtualfs.Time
import com.mvg.virtualfs.storage.FIRST_BLOCK_OFFSET
import com.mvg.virtualfs.storage.FolderEntry
import com.mvg.virtualfs.storage.serialization.serializeToChannel
import java.nio.channels.SeekableByteChannel

class ActiveCoreFileSystem(private val superGroup: SuperGroupAccessor,
                           private val blockGroups: Array<AllocatingBlockGroup>,
                           private val serializer: FileSystemSerializer,
                           private val lockManager: LockManager<Int>,
                           override val time: Time) : CoreFileSystem {

    private val blockGroupSize = superGroup.blockSize * superGroup.blockPerGroup

    fun getOrCreateRootFolder() : Either<CoreFileSystemError, ItemHandler> {
        return when(val r = getInodeItemDescriptor(0)){
            is Either.Right -> initializeItemHandler(NamedItemDescriptor("",r.b))
            is Either.Left -> createRootFolder()
        }
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
                { inode, descriptor -> ActiveFolderHandler(inode, this, descriptor)
                }) as Either<CoreFileSystemError, FolderHandler>
    }

    private fun createFolder(name: String) : Either<CoreFileSystemError, FolderHandler>{
        return createItem(name, ItemType.Folder,
                { initFolder(it) },
                { inode, descriptor -> ActiveFolderHandler(inode, this, descriptor)
                }) as Either<CoreFileSystemError, FolderHandler>
    }

    private fun createFile(name: String) : Either<CoreFileSystemError, FileHandler>{
        return createItem(name, ItemType.File,
                { Unit.right() },
                { inode, descriptor -> ActiveFileHandler(inode, this, descriptor)
                }) as Either<CoreFileSystemError, FileHandler>
    }

    private fun createItem(
            name: String,
            itemType: ItemType,
            initAction: (INodeAccessor) -> Either<CoreFileSystemError, Unit>,
            factoryMethod: (INodeAccessor, descriptor: NamedItemDescriptor) -> ItemHandler)
                : Either<CoreFileSystemError, ItemHandler>{
        //init
        val inode = when (val r = reserveInode()){
            is Either.Left -> return r
            is Either.Right -> r.b
        }
        val blockOffset = when(val r = reserveBlockAndGetOffset(inode.id)){
            is Either.Left -> { freeInode(inode); return r}
            is Either.Right -> r.b
        }
        when(val r = inode.addDataBlock(this, blockOffset)){
            is Either.Left -> return r
        }
        when(val r = initAction(inode)){
            is Either.Left -> {
                freeInode(inode)
                freeBlock(blockOffset)
                return r
            }
        }
        return factoryMethod(inode, NamedItemDescriptor(inode.id, itemType, inode.attributeSet, name)).right()
    }

    private fun initFolder(inode: INodeAccessor) : Either<CoreFileSystemError, Unit> {
        inode.type = NodeType.Folder
        val terminatingEntry = FolderEntry.TerminatingEntry

        val ch = inode.getSeekableByteChannel(this)
        serializeToChannel(ch, terminatingEntry)
        serializer.runSerializationAction {
            inode.serialize(it)
        }
        return Unit.right()
    }

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
        val blockGroupIndex = getInodeBlockGroupIndex(entry.nodeId)
        val itemLock = lockManager.getLockForItem(entry.nodeId)

        if(!itemLock.tryLock())
        {
            return CoreFileSystemError.ItemAlreadyOpenedError.left()
        }
        val acc = when(val r = blockGroups[blockGroupIndex].acquireInode(entry.nodeId, itemLock)){
            is Either.Left -> {itemLock.unlock(); return r}
            is Either.Right -> r.b
        }

        return when(entry.type){
            ItemType.Folder -> ActiveFolderHandler(acc, this, entry).right()
            ItemType.File -> ActiveFileHandler(acc, this, entry).right()
        }
    }

    override fun getInodeItemDescriptor(inodeId: Int): Either<CoreFileSystemError, ItemDescriptor> {
        val blockGroupIndex = getInodeBlockGroupIndex(inodeId)
        val bg = blockGroups[blockGroupIndex]
        return bg.getInode(inodeId)
    }

    override fun createItemHandler(type: ItemType, name: String): Either<CoreFileSystemError, ItemHandler> {
        return when(type){
            ItemType.Folder -> createFolder(name)
            ItemType.File -> createFile(name)
        }
    }

    private fun reserveInode(): Either<CoreFileSystemError, INodeAccessor> {
        val lock = lockManager.createFreeLock()
        lock.lock()
        return when(val r = reserveAndGetItem(0, { it.reserveInode(this, lock) }, CoreFileSystemError.CantCreateMoreItemsError))
        {
            is Either.Left -> r
            is Either.Right -> {
                lockManager.registerLockForItem(r.b.id, lock)
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

    override fun runSerializationAction(action: (SeekableByteChannel) -> Unit) {
        serializer.runSerializationAction(action)
    }

    override fun close() {
        // TODO("Not yet implemented")
        // release all handlers forcefully
        serializer.close()
    }

    private fun getDataBlockGroupIndex(offset: Long) = ((offset - FIRST_BLOCK_OFFSET) / blockGroupSize).toInt()

    private fun getInodeBlockGroupIndex(inodeId: Int) = inodeId / superGroup.inodesPerGroup
}