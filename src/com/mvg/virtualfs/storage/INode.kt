package com.mvg.virtualfs.storage

import java.util.*

enum class NodeType(type: Byte){
    Folder(0),
    File(1)
}

data class INode(
        val id: Int,
        val type: NodeType,
        val created: Date,
        var lastModified: Date,
        var blockOffsets: LongArray) {
}