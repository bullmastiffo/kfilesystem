package com.mvg.virtualfs.core

import arrow.core.Either
import com.mvg.virtualfs.Time
import java.io.Closeable

/**
 * Provides core capabilities to run virtual file system
 * @property time Time
 */
interface CoreFileSystem : FileSystemSerializer, Closeable  {
    /**
     * Time provider
     */
    val time: Time

    /**
     * Reserves data block and returns its offset in containing stream.
     * @param inodeId Int
     * @return Either<CoreFileSystemError, Long>
     */
    fun reserveBlockAndGetOffset(inodeId: Int): Either<CoreFileSystemError, Long>

    /**
     * Frees the block at given offset.
     * @param offset Long
     * @return Either<CoreFileSystemError, Unit>
     */
    fun freeBlock(offset: Long): Either<CoreFileSystemError, Unit>

    /**
     * Initializes item handler for existing item.
     * @param entry NamedItemDescriptor
     * @return Either<CoreFileSystemError, ItemHandler>
     */
    fun initializeItemHandler(entry: NamedItemDescriptor): Either<CoreFileSystemError, ItemHandler>

    /**
     * Gets item descriptor for existing item without obtaining handler.
     * @param inodeId Int
     * @return Either<CoreFileSystemError, ItemDescriptor>
     */
    fun getInodeItemDescriptor(inodeId: Int): Either<CoreFileSystemError, ItemDescriptor>

    /**
     * Creates item and returns associated ItemHandler
     * @param type ItemType
     * @param name String
     * @return Either<CoreFileSystemError, ItemHandler>
     */
    fun createItem(type: ItemType, name: String): Either<CoreFileSystemError, ItemHandler>

    /**
     * Deletes existing item, freeing all allocated resources (inode and data blocks)
     * @param descriptor ItemDescriptor
     */
    fun deleteItem(descriptor: ItemDescriptor) : Either<CoreFileSystemError, Unit>
}