package com.mvg.virtualfs.core

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
}