package com.mvg.virtualfs.storage

import java.util.*

enum class NodeType(type: Byte){
    None(0),
    Folder(1),
    File(2)
}

data class INode(
        val id: Int,
        val type: NodeType,
        val created: Date?,
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