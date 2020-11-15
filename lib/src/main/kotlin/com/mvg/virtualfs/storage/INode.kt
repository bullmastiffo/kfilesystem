package com.mvg.virtualfs.storage

import com.mvg.virtualfs.core.NodeType
import com.mvg.virtualfs.storage.serialization.OutputChannelSerializable
import java.util.*

@OutputChannelSerializable(with = INodeSerializer::class)
class INode(
        val id: Int,
        var type: NodeType,
        var created: Date?,
        var lastModified: Date?,
        val blockOffsets: LongArray) {

    constructor(_id: Int,
                _type: NodeType,
                _created: Date?,
                _lastModified: Date?) : this(_id, _type, _created, _lastModified, LongArray(OFFSETS_SIZE))
    {}

    companion object
    {
        const val OFFSETS_SIZE : Int = 13
        fun sizeInBytes(): Int {
            // Int.SIZE_BYTES + Byte.SIZE_BYTES + 2 * Long.SIZE_BYTES + OFFSETS_SIZE * Long.SIZE_BYTES
            return 125
        }
    }
}