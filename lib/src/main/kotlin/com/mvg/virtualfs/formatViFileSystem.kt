package com.mvg.virtualfs

import com.mvg.virtualfs.storage.BlockGroup
import com.mvg.virtualfs.storage.*
import com.mvg.virtualfs.storage.SuperGroup
import com.mvg.virtualfs.storage.serialization.serializeToChannel
import java.nio.channels.SeekableByteChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

private const val SIZE_4KB = 0x1000

/**
 * Instantiates virtual filesystem based on provided settings in given channel as a storage.
 * @param it SeekableByteChannel
 * @param settings ViFileSystemSettings
 */
fun formatViFileSystem(it: SeekableByteChannel, settings: ViFileSystemSettings) {
    val blockSize = settings.blockSize?.size ?: SIZE_4KB
    val blocksPerGroup = 8 * blockSize
    val blockGroupSize = blocksPerGroup * blockSize
    val totalBlockGroups = (settings.size / blockGroupSize).toInt()
    val totalBlocks = settings.size / blockSize
    val denominator = if (blockSize >= SIZE_4KB) {
        1
    } else {
        SIZE_4KB / blockSize
    }
    val totalNodes = (totalBlocks / denominator).toInt()

    if(totalBlocks > Int.MAX_VALUE)
        throw IllegalArgumentException("size / blockSize must be less than 2^31-1")

    if (totalBlockGroups < 1)
        throw IllegalArgumentException("size must be at least more than 8 * blockSize * blockSize")

    val inodesPerGroup = totalNodes / totalBlockGroups
    val dataBlocksPerGroup = blocksPerGroup - BlockGroup.sizeInBlocks(blockSize, blocksPerGroup, inodesPerGroup)

    var offset = FIRST_BLOCK_OFFSET;

    var inodesIndex = 0
    for(blockNumber in 0 until totalBlockGroups) {
        it.position(offset)
        val block = BlockGroup(blockSize, dataBlocksPerGroup, inodesPerGroup, offset, inodesIndex)
        serializeToChannel(it, block)
        offset+= blockGroupSize
        inodesIndex+=inodesPerGroup
    }

    // allocate full size on channel
    it.position(offset-1)
    it.writeByte(0)

    val superBlock = SuperGroup(
            blockSize,
            totalBlocks.toInt(), totalNodes,
            totalBlockGroups,
            dataBlocksPerGroup * totalBlockGroups,
            totalNodes,
            blocksPerGroup, inodesPerGroup)
    it.position(0)
    serializeToChannel(it, superBlock)
}

/**
 * Creates file at given filePath and instantiates virtual filesystem based on provided settings
 * @param filePath Path
 * @param settings ViFileSystemSettings
 */
fun formatViFileSystem(filePath: Path, settings: ViFileSystemSettings) {
    Files.newByteChannel(filePath, StandardOpenOption.CREATE_NEW, StandardOpenOption.READ, StandardOpenOption.WRITE).use {
        formatViFileSystem(it, settings)
    }
}

