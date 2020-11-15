package com.mvg.virtualfs.storage
import com.mvg.virtualfs.storage.serialization.*

@OutputChannelSerializable(with = SuperGroupSerializer::class)
data class SuperGroup(
        val blockSize: Int,
        val totalBlocks: Int,
        val totalInodes: Int,
        val totalBlockGroups: Int,
        var freeBlockCount: Int,
        var freeInodeCount: Int,
        val blockPerGroup: Int,
        val inodesPerGroup: Int)