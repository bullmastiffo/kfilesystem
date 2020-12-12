package com.mvg.virtualfs.core

import arrow.core.Either
import java.io.Closeable
import java.nio.channels.SeekableByteChannel
import java.util.concurrent.locks.Lock

/**
 * Provides instruments to work with inode instance
 * @property id Int Identifier of current inode
 * @property type NodeType Node type of current inode
 */
interface INodeAccessor {
    val id: Int
    var type: NodeType
    val attributeSet: AttributeSet
    val size: Long

    /**
     * Adds initial data block offset to given inode.
     * @param offset Long
     * @return Either<CoreFileSystemError, Unit>
     */
    fun addInitialDataBlock(offset: Long) : Either<CoreFileSystemError, Unit>

    /**
     * Removes initial data block offset from given inode and returns its offset.
     * @return Either<CoreFileSystemError, Long>
     */
    fun removeInitialDataBlock() : Either<CoreFileSystemError, Long>

    /**
     * Serializes inode to channel at current position.
     * @param channel DuplexChannel
     */
    fun serialize(channel: SeekableByteChannel)

    /**
     * Gets input channel over the data in associated data blocks
     * @param coreFileSystem CoreFileSystem
     * @param acquiredLock Lock already acquired to work with stream, will be released on channel Close
     * @return SeekableByteChannel
     */
    fun getSeekableByteChannel(coreFileSystem: CoreFileSystem, acquiredLock: Lock): SeekableByteChannel
}