package com.mvg.virtualfs.core

import arrow.core.Either
import java.util.concurrent.locks.Lock

/**
 * Strategy defines how FolderHandler works with folder items list
 */
interface FolderItemsStrategy {
    /**
     * Checks if item with given name exists in folder
     * @param name String
     * @param cfs CoreFileSystem
     * @param accessor INodeAccessor
     * @return Either<CoreFileSystemError, Boolean>
     */
    fun containsItem(name: String, cfs: CoreFileSystem, accessor: INodeAccessor)
        : Either<CoreFileSystemError, Boolean>

    /**
     * Returns list of folder items
     * @param cfs CoreFileSystem
     * @param accessor INodeAccessor
     * @param lock Lock
     * @return Either<CoreFileSystemError, Iterable<NamedItemDescriptor>>
     */
    fun listItems(cfs: CoreFileSystem, accessor: INodeAccessor, lock: Lock)
        : Either<CoreFileSystemError, Iterable<NamedItemDescriptor>>

    /**
     * Gets item descriptor for given name, if it exists, otherwise CoreFileSystemError
     * @param name String
     * @param cfs CoreFileSystem
     * @param accessor INodeAccessor
     * @return Either<CoreFileSystemError, NamedItemDescriptor>
     */
    fun getItem(name: String, cfs: CoreFileSystem, accessor: INodeAccessor)
        : Either<CoreFileSystemError, NamedItemDescriptor>

    /**
     * Adds item with given descriptor to folder items list.
     * Warning! Method assumes that item with given name DOES NOT exist, caller is responsible for check.
     * @param descriptor NamedItemDescriptor
     * @param cfs CoreFileSystem
     * @param accessor INodeAccessor
     * @return Either<CoreFileSystemError, NamedItemDescriptor>
     */
    fun addItem(descriptor: NamedItemDescriptor, cfs: CoreFileSystem, accessor: INodeAccessor)
        : Either<CoreFileSystemError, NamedItemDescriptor>

    /**
     * Deletes item with given name from folder items list.
     * Warning! Method assumes that item with given name DOES exist, caller is responsible for check.
     * @param name String
     * @param cfs CoreFileSystem
     * @param accessor INodeAccessor
     * @return Either<CoreFileSystemError, Unit>
     */
    fun deleteItem(name: String, cfs: CoreFileSystem, accessor: INodeAccessor)
            : Either<CoreFileSystemError, Unit>
}