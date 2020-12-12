package com.mvg.virtualfs.core

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.mvg.virtualfs.ViFileSystem
import com.mvg.virtualfs.storage.FolderEntry
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class ActiveFolderHandler(
        private val inodeAccessor: INodeAccessor,
        private val coreFileSystem: CoreFileSystem,
        private val folderItemsStrategy: FolderItemsStrategy,
        override val descriptor: NamedItemDescriptor) : FolderHandler {
    private var isClosed = AtomicBoolean(false)

    private val lock = ReentrantReadWriteLock()

    override fun listItems(): Either<CoreFileSystemError, Iterable<NamedItemDescriptor>> {
        ensureOpened().mapLeft {
            return it.left()
        }

        lock.readLock().lock()
        return folderItemsStrategy.listItems(coreFileSystem, inodeAccessor, lock.readLock())
    }

    override fun getItem(name: String): Either<CoreFileSystemError, ItemHandler> {
        ensureOpened().mapLeft {
            return it.left()
        }
        lock.read {
            return folderItemsStrategy.getItem(name, coreFileSystem, inodeAccessor, lock.readLock()).flatMap {
                coreFileSystem.initializeItemHandler(it)
            }
        }
    }

    override fun createItem(item: ItemTemplate): Either<CoreFileSystemError, ItemHandler> {
        if(!checkNameIsValid(item.name))
            return CoreFileSystemError.InvalidItemNameError.left()
        ensureOpened().mapLeft {
            return it.left()
        }
        lock.write {
            folderItemsStrategy.containsItem(item.name, coreFileSystem, inodeAccessor, lock.writeLock())
                    .fold( {return it.left() } ){
                        if(it)
                        {
                            return CoreFileSystemError.ItemWithSameNameAlreadyExistsError.left()
                        }
                    }
            return coreFileSystem.createItem(item.type, item.name).flatMap { h ->
                folderItemsStrategy.addItem(h.descriptor, coreFileSystem, inodeAccessor, lock.writeLock()).mapLeft {
                    return it.left()
                }
                h.right()
            }
        }
    }

    override fun deleteItem(name: String): Either<CoreFileSystemError, Unit> {
        ensureOpened().mapLeft {
            return it.left()
        }
        lock.write {
            return folderItemsStrategy.getItem(name, coreFileSystem, inodeAccessor, lock.writeLock())
                .flatMap { item ->
                    coreFileSystem.initializeItemHandler(item).flatMap {
                        it.delete()
                    }.mapLeft { return it.left() }
                    return folderItemsStrategy.deleteItem(name, coreFileSystem, inodeAccessor, lock.writeLock())
                }
        }
    }

    override fun delete(): Either<CoreFileSystemError, Unit> {
        ensureOpened().mapLeft {
            return it.left()
        }
        if (inodeAccessor.attributeSet.size > FolderEntry.TerminatingEntrySize){
            return CoreFileSystemError.CantDeleteNonEmptyFolderError.left()
        }
        val writeLock = lock.writeLock()
        if (!writeLock.tryLock()){
            return CoreFileSystemError.ItemAlreadyOpenedError.left()
        }
        inodeAccessor.getSeekableByteChannel(coreFileSystem, writeLock).truncate(0L)
        return coreFileSystem.deleteItem(inodeAccessor)
    }

    override fun close() {
        isClosed.set(true)
    }

    private fun ensureOpened() : Either<CoreFileSystemError, Unit> {
        if (isClosed.get()){
            return CoreFileSystemError.ItemClosedError.left()
        }
        return Unit.right()
    }

    companion object {
        fun checkNameIsValid(name: String) : Boolean{
            return !name.contains(ViFileSystem.PATH_DELIMITER)
                    && name.length in 1..ViFileSystem.MAX_FILENAME_LENGTH
        }
    }
}

