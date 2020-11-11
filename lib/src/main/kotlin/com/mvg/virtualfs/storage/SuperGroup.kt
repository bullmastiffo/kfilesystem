package com.mvg.virtualfs.storage
import kotlinx.serialization.*

@Serializable
data class SuperGroup(
        val totalBlocks: Int,
        val totalInodes: Int,
        var freeBlockCount: Int,
        var freeInodeCount: Int,
        val blockPerGroup: Int,
        val inodesPerGroup: Int)