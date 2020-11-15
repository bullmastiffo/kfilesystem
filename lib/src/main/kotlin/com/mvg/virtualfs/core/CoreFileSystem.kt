package com.mvg.virtualfs.core

import arrow.core.Either

interface CoreFileSystem  {
    fun reserveBlockAndGetOffset(inodeId: Int): Either<CoreFileSystemError, Long>
    fun freeBlock(offset: Long): Either<CoreFileSystemError, Unit>
    fun initializeItemHandler(entry: NamedItemDescriptor): Either<CoreFileSystemError, ItemHandler>
    fun getInodeItemDescriptor(inodeId: Int): Either<CoreFileSystemError, ItemDescriptor>
    fun reserveInode(): Either<CoreFileSystemError, INodeAccessor>
    fun freeInode(nodeAccessor: INodeAccessor): Either<CoreFileSystemError, Unit>
}