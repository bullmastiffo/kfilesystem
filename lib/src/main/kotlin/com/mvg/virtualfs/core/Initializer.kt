package com.mvg.virtualfs.core

import arrow.core.Either
import com.mvg.virtualfs.SystemTime
import com.mvg.virtualfs.storage.FIRST_BLOCK_OFFSET
import com.mvg.virtualfs.storage.SuperGroup
import com.mvg.virtualfs.storage.serialization.DuplexChannel
import com.mvg.virtualfs.storage.serialization.deserializeFromChannel

fun initializeViFilesystem(channel: DuplexChannel): Either<CoreFileSystemError, ViFileSystem>{
    if(channel.position != 0L){
        channel.position(0L)
    }
    val sg = deserializeFromChannel<SuperGroup>(channel)
    val accessor = ActiveSuperGroupAccessor(sg)
    val blockGroupSize = (sg.blockPerGroup * sg.blockSize).toLong()

    var offset = FIRST_BLOCK_OFFSET
    val blockGroups = Array<FileSystemAllocator>(sg.totalBlockGroups){
        channel.position(offset)
        offset += blockGroupSize
        ActiveAllocatingBlockGroup(it, deserializeFromChannel(channel))
    }
    var coreFs = ActiveCoreFileSystem(
            accessor,
            blockGroups,
            DuplexChannelFileSystemSerializer(channel),
            PrimitiveLockManager<Int>(),
            SystemTime)
    var rootFolder = coreFs.getOrCreateRootFolder()
    return when(rootFolder){
        is Either.Left -> rootFolder
        is Either.Right -> Either.Right(ViFileSystem(rootFolder.b as FolderHandler, coreFs))
    }
}

