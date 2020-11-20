package com.mvg.virtualfs.core

import java.nio.channels.SeekableByteChannel

interface SuperGroupAccessor {
    val blockSize: Int
    val totalBlocks: Int
    val totalInodes: Int
    val totalBlockGroups: Int
    val freeBlockCount: Int
    val freeInodeCount: Int
    val blockPerGroup: Int
    val inodesPerGroup: Int

    fun incrementFreeBlockCount()
    fun decrementFreeBlockCount()

    fun incrementFreeInodeCount()
    fun decrementFreeInodeCount()

    fun serialize(channel: SeekableByteChannel)
}