package com.mvg.virtualfs.core

import arrow.core.Either
import com.mvg.virtualfs.Time
import java.io.Closeable

interface CoreFileSystem : FileSystemSerializer, Closeable  {
    val time: Time
    fun reserveBlockAndGetOffset(inodeId: Int): Either<CoreFileSystemError, Long>
    fun freeBlock(offset: Long): Either<CoreFileSystemError, Unit>
    fun initializeItemHandler(entry: NamedItemDescriptor): Either<CoreFileSystemError, ItemHandler>
    fun getInodeItemDescriptor(inodeId: Int): Either<CoreFileSystemError, ItemDescriptor>
    fun reserveInode(): Either<CoreFileSystemError, INodeAccessor>
    fun freeInode(nodeAccessor: INodeAccessor): Either<CoreFileSystemError, Unit>
}