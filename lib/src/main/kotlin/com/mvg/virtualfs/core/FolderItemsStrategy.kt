package com.mvg.virtualfs.core

import arrow.core.Either
import java.util.concurrent.locks.Lock

interface FolderItemsStrategy {
    fun containsItem(name: String, cfs: CoreFileSystem, accessor: INodeAccessor)
        : Either<CoreFileSystemError, Boolean>

    fun listItems(cfs: CoreFileSystem, accessor: INodeAccessor, lock: Lock)
        : Either<CoreFileSystemError, Iterable<NamedItemDescriptor>>

    fun getItem(name: String, cfs: CoreFileSystem, accessor: INodeAccessor)
        : Either<CoreFileSystemError, NamedItemDescriptor>

    fun addItem(descriptor: NamedItemDescriptor, cfs: CoreFileSystem, accessor: INodeAccessor)
        : Either<CoreFileSystemError, NamedItemDescriptor>

    fun deleteItem(name: String, cfs: CoreFileSystem, accessor: INodeAccessor)
            : Either<CoreFileSystemError, Unit>
}