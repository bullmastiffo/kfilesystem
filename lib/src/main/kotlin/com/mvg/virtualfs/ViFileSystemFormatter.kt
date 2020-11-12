package com.mvg.virtualfs

import com.mvg.virtualfs.storage.BlockGroup
import com.mvg.virtualfs.storage.SuperGroup
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

fun FormatViFileSystem(filePath: Path, settings: ViFileSystemSettings) {
    val SIZE_4KB = 0x1000

    val blockSize = settings.blockSize?.ordinal ?: SIZE_4KB
    val blocksPerGroup = 8 * blockSize
    val blockGroupSize = blocksPerGroup * blockSize
    val totalBlockGroups = settings.size / blockGroupSize
    val totalBlocks = settings.size / blockSize

    if(totalBlocks > Int.MAX_VALUE)
        throw IllegalArgumentException("size / blockSize must be less than 2^31-1")

    if (totalBlockGroups < 1)
        throw IllegalArgumentException("size must be at least more than 8 * blockSize * blockSize")

    val denominator = if (blockSize >= SIZE_4KB) {
        1
    } else {
        SIZE_4KB / blockSize
    }

    val totalNodes = (totalBlocks / denominator).toInt()
    val inodesPerGroup = (totalNodes / totalBlockGroups).toInt()

    Files.newByteChannel(filePath, StandardOpenOption.CREATE_NEW, StandardOpenOption.READ, StandardOpenOption.WRITE).use {
        byteChannel -> {
        val superBlock = SuperGroup(
                totalBlocks.toInt(), totalNodes,
                0,0,
                blocksPerGroup, inodesPerGroup)

    }
    }

    val templateBlock = BlockGroup(blockSize, blocksPerGroup, inodesPerGroup, 0, 0)
    // TODO calculate and add last block group for remainder blocks
}

