package com.mvg.virtualfs.storage

import kotlinx.serialization.Serializable
import java.util.*

enum class NodeType(val type: Byte){
    None(0),
    Folder(1),
    File(2)
}

@Serializable
data class INode(
        val id: Int,
        val type: NodeType,
        @Serializable(with = DateAsLongSerializer::class)
        val created: Date?,
        @Serializable(with = DateAsLongSerializer::class)
        var lastModified: Date?) {
    val blockOffsets: LongArray = LongArray(OFFSETS_SIZE)

    companion object
    {
        const val OFFSETS_SIZE : Int = 13
        fun sizeInBytes(): Int {
            // Int.SIZE_BYTES + Byte.SIZE_BYTES + 2 * Long.SIZE_BYTES + OFFSETS_SIZE * Long.SIZE_BYTES
            return 125
        }
    }
}