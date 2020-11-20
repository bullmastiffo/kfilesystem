package com.mvg.virtualfs.storage
import com.mvg.virtualfs.storage.serialization.*

/**
 * Descriptor of super group, holding core information about virtual file system
 * @property blockSize Int Filesystem block size
 * @property totalBlocks Int Total blocks count
 * @property totalInodes Int Total inodes count
 * @property totalBlockGroups Int Total block groups count
 * @property freeBlockCount Int Free blocks count
 * @property freeInodeCount Int Free inodes count
 * @property blockPerGroup Int Number of data blocks per block group
 * @property inodesPerGroup Int Number of inodes per block group
 * @constructor
 */
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