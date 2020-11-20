package com.mvg.virtualfs.storage

import com.mvg.virtualfs.core.NodeType
import com.mvg.virtualfs.storage.serialization.OutputChannelSerializable
import java.util.*

/**
 * Represents descriptor of inode in virtual file system.
 * @property id Int Inode identifier
 * @property type NodeType Type od inode
 * @property dataSize Long Data size store in entity
 * @property created Date? Creation date
 * @property lastModified Date? Last modification date
 * @property blockOffsets LongArray Array with offsets to data blocks
 * @constructor
 */
@OutputChannelSerializable(with = INodeSerializer::class)
class INode(
        val id: Int,
        var type: NodeType,
        var dataSize: Long,
        var created: Date?,
        var lastModified: Date?,
        val blockOffsets: LongArray) {

    constructor(_id: Int,
                _type: NodeType,
                _created: Date?,
                _lastModified: Date?) : this(_id, _type, 0L, _created, _lastModified, LongArray(OFFSETS_SIZE))
    {}

    companion object
    {
        const val OFFSETS_SIZE : Int = 13
        fun sizeInBytes(): Int {
            // Int.SIZE_BYTES + Byte.SIZE_BYTES + 3 * Long.SIZE_BYTES + OFFSETS_SIZE * Long.SIZE_BYTES
            return 133
        }
    }
}