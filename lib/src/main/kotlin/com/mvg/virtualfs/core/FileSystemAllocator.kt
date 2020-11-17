package com.mvg.virtualfs.core

import arrow.core.Either
import java.util.concurrent.locks.Lock

interface FileSystemAllocator: BlockAllocator, InodeAllocator {
    val id: Int
}

interface BlockAllocator{
    val freeBlocks: Int

    fun reserveBlockAndGetOffset(serializer: FileSystemSerializer): Either<CoreFileSystemError, Long>
    fun markBlockFree(serializer: FileSystemSerializer, offset: Long): Either<CoreFileSystemError, Unit>
}

interface InodeAllocator{
    val freeINodes: Int

    fun acquireInode(inodeId: Int, lock: Lock): Either<CoreFileSystemError, INodeAccessor>
    fun getInode(inodeId: Int): Either<CoreFileSystemError, ItemDescriptor>
    fun reserveInode(coreFileSystem: CoreFileSystem, lock: Lock): Either<CoreFileSystemError, INodeAccessor>
    fun markInodeFree(coreFileSystem: CoreFileSystem, inode: INodeAccessor): Either<CoreFileSystemError, Unit>
}
