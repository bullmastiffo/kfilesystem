package com.mvg.virtualfs.core

import arrow.core.Either
import com.mvg.virtualfs.storage.BlockGroup
import java.util.concurrent.locks.Lock

class ActiveAllocatingBlockGroup(private val groupId: Int, private val blockGroup: BlockGroup): FileSystemAllocator {
    override val id: Int
        get() = groupId
    override val freeBlocks: Int
        get() = TODO("Not yet implemented")

    override fun reserveBlockAndGetOffset(serializer: FileSystemSerializer): Either<CoreFileSystemError, Long> {
        TODO("Not yet implemented")
    }

    override fun markBlockFree(serializer: FileSystemSerializer, offset: Long): Either<CoreFileSystemError, Unit> {
        TODO("Not yet implemented")
    }

    override val freeINodes: Int
        get() = TODO("Not yet implemented")

    override fun acquireInode(lock: Lock): Either<CoreFileSystemError, INodeAccessor> {
        TODO("Not yet implemented")
    }

    override fun getInode(id: Int): Either<CoreFileSystemError, ItemDescriptor> {
        TODO("Not yet implemented")
    }

    override fun reserveInode(serializer: FileSystemSerializer, lock: Lock): Either<CoreFileSystemError, INodeAccessor> {
        TODO("Not yet implemented")
    }

    override fun markInodeFree(serializer: FileSystemSerializer, inode: INodeAccessor): Either<CoreFileSystemError, Unit> {
        TODO("Not yet implemented")
    }


}