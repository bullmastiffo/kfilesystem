package com.mvg.virtualfs.core

import arrow.core.Either
import com.mvg.virtualfs.storage.INode
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

    fun acquireInode(lock: Lock): Either<CoreFileSystemError, INodeAccessor>
    fun getInode(id: Int): Either<CoreFileSystemError, ItemDescriptor>
    fun reserveInode(serializer: FileSystemSerializer, lock: Lock): Either<CoreFileSystemError, INodeAccessor>
    fun markInodeFree(serializer: FileSystemSerializer, inode: INodeAccessor): Either<CoreFileSystemError, Unit>
}
