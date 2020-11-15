package com.mvg.virtualfs.core

import arrow.core.Either
import com.mvg.virtualfs.storage.serialization.DuplexChannel
import com.mvg.virtualfs.storage.serialization.InputChannel
import java.io.Closeable

/**
 * Provides instruments to work with inode instance
 * @property id Int Identifier of current inode
 * @property type NodeType Node type of current inode
 */
interface INodeAccessor : Closeable {
    val id: Int
    var type: NodeType
    val attributeSet: AttributeSet

    /**
     * Adds data block to given inode. Returns added offset value on success.
     * @param coreFileSystem CoreFileSystem
     * @param offset Long
     * @return Either<CoreFileSystemError, Long>
     */
    fun addDataBlock(coreFileSystem: CoreFileSystem, offset: Long) : Either<CoreFileSystemError, Long>

    /**
     * Serializes inode to channel at current position.
     * @param channel DuplexChannel
     */
    fun serialize(channel: DuplexChannel)

    /**
     * Gets input channel over the data in associated data blocks
     * @return Either<CoreFileSystemError, DuplexChannel>
     */
    fun getDataInputChannel(coreFileSystem: CoreFileSystem): InputChannel
}