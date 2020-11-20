package com.mvg.virtualfs.core

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.mvg.virtualfs.ViFileSystem
import com.mvg.virtualfs.storage.FolderEntry
import com.mvg.virtualfs.storage.serialization.deserializeFromChannel
import com.mvg.virtualfs.storage.serialization.serializeToChannel
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.withLock
import kotlin.concurrent.write

private val ItemType.toNodeType: NodeType
    get() {
        return when(this){
            ItemType.File -> NodeType.File
            ItemType.Folder -> NodeType.Folder
        }
    }

class ActiveFolderHandler(
        private val inodeAccessor: INodeAccessor,
        private val coreFileSystem: CoreFileSystem,
        override val descriptor: NamedItemDescriptor) : FolderHandler {
    private var isClosed = AtomicBoolean(false)
    private val initLock = ReentrantLock()

    @Volatile
    private var itemsMap: LinkedHashMap<String, NamedItemDescriptor>? = null
    private var lastEntryOffset: Long = 0L
    private val lock = ReentrantReadWriteLock()

    override fun listItems(): Either<CoreFileSystemError, List<NamedItemDescriptor>> {
        when(val r = ensureFolderRead()){
            is Either.Left -> return r
        }
        lock.read {
            val items = ArrayList<NamedItemDescriptor>(itemsMap!!.size)
            itemsMap!!.values.forEach { items.add(it) }
            return items.right()
        }
    }

    override fun getItem(name: String): Either<CoreFileSystemError, ItemHandler> {
        when(val r = ensureFolderRead()){
            is Either.Left -> return r
        }
        lock.read {
            if(itemsMap!!.containsKey(name)) {
                val descriptor = itemsMap!![name]!!
                return coreFileSystem.initializeItemHandler(descriptor)
            }
        }
        return CoreFileSystemError.ItemNotFoundError(name).left()
    }

    override fun createItem(item: ItemTemplate): Either<CoreFileSystemError, ItemHandler> {
        if(!checkNameIsValid(item.name))
            return CoreFileSystemError.InvalidItemNameError.left()
        when(val r = ensureFolderRead()){
            is Either.Left -> return r
        }
        lock.write {
            if(itemsMap!!.containsKey(item.name))
            {
                return CoreFileSystemError.ItemWithSameNameAlreadyExistsError.left()
            }
            var h = when (val r = coreFileSystem.createItem(item.type, item.name))
            {
                is Either.Left -> return r
                is Either.Right -> r.b
            }
            itemsMap!![item.name] = h.descriptor
            val entry = FolderEntry(h.descriptor.nodeId, h.descriptor.type.toNodeType, h.descriptor.name)
            val ch = inodeAccessor.getSeekableByteChannel(coreFileSystem)
            ch.position(lastEntryOffset)
            serializeToChannel(ch, entry)
            lastEntryOffset = ch.position()
            serializeToChannel(ch, FolderEntry.TerminatingEntry)
            return h.right()
        }
    }

    override fun deleteItem(name: String): Either<CoreFileSystemError, Unit> {
        when(val r = ensureFolderRead()){
            is Either.Left -> return r
        }
        lock.write {
            if(!itemsMap!!.containsKey(name))
            {
                return CoreFileSystemError.ItemNotFoundError(name).left()
            }
            val item = itemsMap!![name]!!
            when (val r = coreFileSystem.deleteItem(item))
            {
                is Either.Left -> return r
            }
            itemsMap!!.remove(name)
            val ch = inodeAccessor.getSeekableByteChannel(coreFileSystem)
            ch.position(0)
            itemsMap!!.forEach {
                serializeToChannel(ch, FolderEntry(it.value.nodeId, it.value.type.toNodeType, it.value.name))
            }
            lastEntryOffset = ch.position()
            serializeToChannel(ch, FolderEntry.TerminatingEntry)
            ch.truncate(ch.position())
            return Unit.right()
        }
    }

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
            val channel = inodeAccessor.getSeekableByteChannel(coreFileSystem)
            do {
                val entry = deserializeFromChannel<FolderEntry>(channel)
                if(entry == FolderEntry.TerminatingEntry){
                    lastEntryOffset = channel.position() - FolderEntry.TerminatingEntrySize
                    break
                }
                map[entry.name] = when(val r = coreFileSystem.getInodeItemDescriptor(entry.inodeId))
                {
                    is Either.Right -> NamedItemDescriptor(entry.name, r.b)
                    is Either.Left -> return r
                }
            } while (true)

            itemsMap = map
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

