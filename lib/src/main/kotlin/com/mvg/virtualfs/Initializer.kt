package com.mvg.virtualfs

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.right
import com.mvg.virtualfs.core.*
import com.mvg.virtualfs.storage.FIRST_BLOCK_OFFSET
import com.mvg.virtualfs.storage.SuperGroup
import com.mvg.virtualfs.storage.serialization.deserializeFromChannel
import java.nio.channels.SeekableByteChannel

/**
 * Initializes instance of @see ViFileSystem stored in given channel.
 * @param channel SeekableByteChannel
 * @return Either<CoreFileSystemError, ViFileSystem>
 */
fun initializeViFilesystem(channel: SeekableByteChannel): Either<CoreFileSystemError, ViFileSystem>{
    if(channel.position() != 0L){
        channel.position(0L)
    }
    val sg = deserializeFromChannel<SuperGroup>(channel)
    val accessor = ActiveSuperGroupAccessor(sg)
    val blockGroupSize = (sg.blockPerGroup * sg.blockSize).toLong()

    var offset = FIRST_BLOCK_OFFSET
    val blockGroups = Array<AllocatingBlockGroup>(sg.totalBlockGroups){
        channel.position(offset)
        offset += blockGroupSize
        ActiveAllocatingBlockGroup(it, deserializeFromChannel(channel))
    }
    val coreFs = ActiveCoreFileSystem(
            accessor,
            blockGroups,
            DuplexChannelFileSystemSerializer(channel),
            ReferenceCountingConcurrentPool(ItemHandlerProxyFactory()),
            { cfs, inodeAccessor, descriptor ->
                ActiveFolderHandler(inodeAccessor, cfs, InMemoryFolderItemsStrategy(), descriptor)
            },
            SystemTime)
    return coreFs.getOrCreateRootFolder().flatMap { ViFileSystem(it, coreFs).right() }
}

