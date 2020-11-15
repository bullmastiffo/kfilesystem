package com.mvg.virtualfs.core.folders

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.mvg.virtualfs.core.*
import com.mvg.virtualfs.storage.FolderEntry
import com.mvg.virtualfs.storage.serialization.deserializeFromChannel
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

class ActiveFolderHandler(
        private val inodeAccessor: INodeAccessor,
        private val coreFileSystem: CoreFileSystem,
        private val folderName: String) : FolderHandler {
    private var isClosed = AtomicBoolean(false)
    private val initLock = ReentrantLock()

    @Volatile
    private var itemsMap: LinkedHashMap<String, NamedItemDescriptor>? = null
    private val lock = ReentrantReadWriteLock()

    override fun listItems(): Either<CoreFileSystemError, Iterable<NamedItemDescriptor>> {
        when(val r = ensureFolderRead()){
            is Either.Left -> return r
        }
        lock.readLock().withLock {
            val items = ArrayList<NamedItemDescriptor>(itemsMap!!.size)
            itemsMap!!.values.forEach { items.add(it) }
            return items.right()
        }
    }

    override fun getItem(name: String): Either<CoreFileSystemError, ItemHandler> {
        when(val r = ensureFolderRead()){
            is Either.Left -> return r
        }
        lock.readLock().withLock {
            if(itemsMap!!.containsKey(name)) {
                val descriptor = itemsMap!![name]!!
                return coreFileSystem.initializeItemHandler(descriptor)
            }
        }
        return CoreFileSystemError.ItemNotFoundError(name).left()
    }

    override fun createItem(item: ItemTemplate): ItemHandler {
        TODO("Not yet implemented")
    }

    override val name: String
        get() = folderName
    override val attributes: AttributeSet
        get() = inodeAccessor.attributeSet

    override fun close() {
        if(!isClosed.getAndSet(true)) {
            inodeAccessor.close()
        }
    }

    private fun ensureFolderRead() : Either<CoreFileSystemError, Unit> {
        if (isClosed.get()){
            return CoreFileSystemError.ItemClosedError.left()
        }
        if (itemsMap != null) {
            return Unit.right()
        }
        initLock.withLock {
            if(itemsMap != null)
            {
                return Unit.right()
            }

            val map = LinkedHashMap<String, NamedItemDescriptor>()
            inodeAccessor.getDataInputChannel(coreFileSystem).use {
                do {
                    val entry = deserializeFromChannel<FolderEntry>(it)
                    map[entry.name] = when(val r = coreFileSystem.getInodeItemDescriptor(entry.inodeId))
                    {
                        is Either.Right -> NamedItemDescriptor(entry.name, r.b)
                        is Either.Left -> return r
                    }
                } while (entry != FolderEntry.TerminatingEntry)
            }
            itemsMap = map
        }
        return Unit.right()
    }
}

