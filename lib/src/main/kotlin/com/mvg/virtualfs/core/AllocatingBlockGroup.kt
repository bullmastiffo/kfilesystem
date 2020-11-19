package com.mvg.virtualfs.core

import arrow.core.Either
import java.util.concurrent.locks.Lock

/**
 * Represents instance of allocating block group, that allocates blocks and inodes.
 * @property id Int
 */
interface AllocatingBlockGroup: BlockAllocator, InodeAllocator {
    val id: Int
}

/**
 * Interface to allocate blocks to write data.
 * @property freeBlocks Int
 */
interface BlockAllocator{
    val freeBlocks: Int

    /**
     * Reserves the block for use and returns it's offset
     * @param serializer FileSystemSerializer to write changes to persistent channel
     * @return Either<CoreFileSystemError, Long>
     */
    fun reserveBlockAndGetOffset(serializer: FileSystemSerializer): Either<CoreFileSystemError, Long>

    /**
     *
     * @param serializer FileSystemSerializer
     * @param offset Long
     * @return Either<CoreFileSystemError, Unit>
     */
    fun markBlockFree(serializer: FileSystemSerializer, offset: Long): Either<CoreFileSystemError, Unit>
}

/**
 * Interface to allocate inode to represent items in filesystem (files, folders)
 * @property freeInodes Int
 */
interface InodeAllocator{
    val freeInodes: Int

    /**
     * Acquires already reserved node for access.
     * @param inodeId Int
     * @param lock Lock
     * @return Either<CoreFileSystemError, INodeAccessor>
     */
    fun acquireInode(inodeId: Int, lock: Lock): Either<CoreFileSystemError, INodeAccessor>

    /**
     * Gets metadata of reserved inode without getting access.
     * @param inodeId Int
     * @return Either<CoreFileSystemError, ItemDescriptor>
     */
    fun getInode(inodeId: Int): Either<CoreFileSystemError, ItemDescriptor>

    /**
     * Reserves free inode in the group and returns accessor.
     * @param coreFileSystem CoreFileSystem
     * @param lock Lock
     * @return Either<CoreFileSystemError, INodeAccessor>
     */
    fun reserveInode(coreFileSystem: CoreFileSystem, lock: Lock): Either<CoreFileSystemError, INodeAccessor>

    /**
     * Marks previously reserved Inode as free.
     * @param coreFileSystem CoreFileSystem
     * @param inode INodeAccessor
     * @return Either<CoreFileSystemError, Unit>
     */
    fun markInodeFree(coreFileSystem: CoreFileSystem, inode: INodeAccessor): Either<CoreFileSystemError, Unit>
}
