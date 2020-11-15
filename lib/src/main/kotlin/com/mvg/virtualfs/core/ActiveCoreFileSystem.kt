package com.mvg.virtualfs.core

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.mvg.virtualfs.core.folders.ActiveFolderHandler
import com.mvg.virtualfs.storage.FIRST_BLOCK_OFFSET
import com.mvg.virtualfs.storage.FolderEntry
import com.mvg.virtualfs.storage.serialization.serializeToChannel

class ActiveCoreFileSystem(private val superGroup: SuperGroupAccessor,
                           private val blockGroups: Array<FileSystemAllocator>,
                           private val serializer: FileSystemSerializer,
                           private val lockManager: LockManager<Int>) : CoreFileSystem {

    private val blockGroupSize = superGroup.blockSize * superGroup.blockPerGroup

    fun getOrCreateRootFolder() : Either<CoreFileSystemError, ItemHandler> {
        return when(val r = getInodeItemDescriptor(0)){
            is Either.Right -> initializeItemHandler(NamedItemDescriptor("",r.b))
            is Either.Left -> createRootFolder()
        }
    }

    private fun createRootFolder() : Either<CoreFileSystemError, FolderHandler>{
        //init
        val inode = when (val r = reserveInode()){
            is Either.Left -> return r
            is Either.Right -> r.b
        }
        if(inode.id != 0){
            freeInode(inode)
            return Either.Left(CoreFileSystemError.FileSystemCorruptedError)
        }
        val blockOffset = when(val r = reserveBlockAndGetOffset(inode.id)){
            is Either.Left -> { freeInode(inode); return r}
            is Either.Right -> r.b
        }
        superGroup.decrementFreeInodeCount()
        when(val r = initFolder(inode, blockOffset)){
            is Either.Left -> {
                freeInode(inode)
                freeBlock(blockOffset)
                return r
            }
        }
        return ActiveFolderHandler(inode, this, "").right()
    }

    private fun initFolder(inode: INodeAccessor, offset: Long) : Either<CoreFileSystemError, INodeAccessor> {
        inode.type = NodeType.Folder
        when(val r = inode.addDataBlock(this, offset)){
            is Either.Left -> return r
        }
        val terminatingEntry = FolderEntry.TerminatingEntry

        serializer.runSerializationAction {
            inode.serialize(it)
            serializeToChannel(it.position(offset), terminatingEntry)
        }
        return Either.Right(inode)
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
        val acc = when(val r = blockGroups[blockGroupIndex].acquireInode(itemLock)){
            is Either.Left -> {itemLock.unlock(); return r}
            is Either.Right -> r.b
        }

        return when(entry.type){
            ItemType.Folder -> ActiveFolderHandler(acc, this, "").right()
            ItemType.File -> TODO("Create FileHandlers")
        }
    }

    override fun getInodeItemDescriptor(inodeId: Int): Either<CoreFileSystemError, ItemDescriptor> {
        val blockGroupIndex = getInodeBlockGroupIndex(inodeId)
        val bg = blockGroups[blockGroupIndex]
        return bg.getInode(inodeId)
    }

    override fun reserveInode(): Either<CoreFileSystemError, INodeAccessor> {
        val lock = lockManager.createFreeLock()
        lock.lock()
        return when(val r = reserveAndGetItem(0, { it.reserveInode(serializer, lock) }, CoreFileSystemError.CantCreateMoreItemsError))
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
            blockGetter: (FileSystemAllocator) -> Either<CoreFileSystemError, T>,
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

    override fun freeInode(nodeAccessor: INodeAccessor): Either<CoreFileSystemError, Unit> {
        val blockGroupIndex = getInodeBlockGroupIndex(nodeAccessor.id)
        when (val r = blockGroups[blockGroupIndex].markInodeFree(serializer, nodeAccessor))
        {
            is Either.Left -> return r
        }
        superGroup.incrementFreeInodeCount()
        return Either.Right(Unit)
    }

    private fun getDataBlockGroupIndex(offset: Long) = ((offset - FIRST_BLOCK_OFFSET) / blockGroupSize).toInt()

    private fun getInodeBlockGroupIndex(inodeId: Int) = inodeId / superGroup.inodesPerGroup
}