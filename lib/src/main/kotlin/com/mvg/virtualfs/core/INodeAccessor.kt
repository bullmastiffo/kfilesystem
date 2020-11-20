package com.mvg.virtualfs.core

import arrow.core.Either
import java.io.Closeable
import java.nio.channels.SeekableByteChannel

/**
 * Provides instruments to work with inode instance
 * @property id Int Identifier of current inode
 * @property type NodeType Node type of current inode
 */
interface INodeAccessor : Closeable {
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
     * Serializes inode to channel at current position.
     * @param channel DuplexChannel
     */
    fun serialize(channel: SeekableByteChannel)

    /**
     * Gets input channel over the data in associated data blocks
     * @return Either<CoreFileSystemError, DuplexChannel>
     */
    fun getSeekableByteChannel(coreFileSystem: CoreFileSystem): SeekableByteChannel
}