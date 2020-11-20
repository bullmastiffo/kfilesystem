package com.mvg.virtualfs

data class FileSystemInfo(
        val totalSize: Long,
        val freeSize: Long,
        val freeInodes: Int,
        val freeBlocks: Int,
        val blockSize: Int,
)