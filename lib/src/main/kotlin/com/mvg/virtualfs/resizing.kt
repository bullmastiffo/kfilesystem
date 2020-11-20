package com.mvg.virtualfs

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.mvg.virtualfs.core.*
import com.mvg.virtualfs.storage.BlockGroup
import com.mvg.virtualfs.storage.FIRST_BLOCK_OFFSET
import com.mvg.virtualfs.storage.SuperGroup
import com.mvg.virtualfs.storage.serialization.deserializeFromChannel
import com.mvg.virtualfs.storage.serialization.serializeToChannel
import com.mvg.virtualfs.storage.writeByte
import java.nio.channels.SeekableByteChannel

/***
 * Resizes existing ViFilesystem stored in provided channel to the given new size.
 * @param channel SeekableByteChannel
 * @param newSize Long
 * @return Either<CoreFileSystemError, Unit>
 */
fun resizeViFilesystem(channel: SeekableByteChannel, newSize: Long): Either<CoreFileSystemError, Unit> {
    if(channel.position() != 0L){
        channel.position(0L)
    }
    val sg = deserializeFromChannel<SuperGroup>(channel)
    val currentSize = sg.totalBlocks.toLong() * sg.blockSize

    if (currentSize == newSize)
        return Unit.right()

    if (newSize < currentSize){
        return CoreFileSystemError.UnsupportedResizeOperationError("Downsizing is not supported. Current size is $currentSize").left()
    }
    val totalBlocks = newSize / sg.blockSize
    val blockGroupSize = sg.blockPerGroup * sg.blockSize
    val blockGroupsToAdd = ((newSize - currentSize) / blockGroupSize).toInt()
    val newTotalBlockGroups = sg.totalBlockGroups + blockGroupsToAdd
    val totalNodes = sg.inodesPerGroup * newTotalBlockGroups

    if (blockGroupsToAdd == 0){
        return CoreFileSystemError.UnsupportedResizeOperationError("Can't resize partition. Minimum expansion step is $blockGroupSize bytes.").left()
    }
    if(totalBlocks > Int.MAX_VALUE) {
        return CoreFileSystemError.UnsupportedResizeOperationError("Can't resize partition. Number of inodes must be less than 2^31-1").left()
    }

    var offset = FIRST_BLOCK_OFFSET + sg.totalBlockGroups * blockGroupSize;
    val dataBlocksPerGroup = sg.blockPerGroup - BlockGroup.sizeInBlocks(sg.blockSize, sg.blockPerGroup, sg.inodesPerGroup)

    var inodesIndex = sg.totalInodes
    for(blockNumber in sg.totalBlockGroups until newTotalBlockGroups) {
        channel.position(offset)
        val block = BlockGroup(sg.blockSize, dataBlocksPerGroup, sg.inodesPerGroup, offset, inodesIndex)
        serializeToChannel(channel, block)
        offset+= blockGroupSize
        inodesIndex+= sg.inodesPerGroup
    }
    // allocate full size on channel
    channel.position(offset-1)
    channel.writeByte(0)

    val superBlock = SuperGroup(
            sg.blockSize,
            totalBlocks.toInt(),
            totalNodes,
            newTotalBlockGroups,
            sg.freeBlockCount + dataBlocksPerGroup * blockGroupsToAdd,
            sg.freeInodeCount + sg.inodesPerGroup * blockGroupsToAdd,
            sg.blockPerGroup, sg.inodesPerGroup)
    channel.position(0)
    serializeToChannel(channel, superBlock)
    return Unit.right()
}

