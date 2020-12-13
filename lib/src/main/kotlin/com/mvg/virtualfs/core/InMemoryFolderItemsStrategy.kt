package com.mvg.virtualfs.core

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.mvg.virtualfs.storage.FolderEntry
import com.mvg.virtualfs.storage.serialization.deserializeFromChannel
import com.mvg.virtualfs.storage.serialization.serializeToChannel
import java.nio.channels.ReadableByteChannel
import java.nio.channels.WritableByteChannel
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal val ItemType.toNodeType: NodeType
    get() {
        return when(this){
            ItemType.File -> NodeType.File
            ItemType.Folder -> NodeType.Folder
        }
    }

class InMemoryFolderItemsStrategy(
        private val serialize: (channel: WritableByteChannel, value: FolderEntry) -> Unit
            = { ch, e -> serializeToChannel(ch, e) },
        private val deserialize: (channel: ReadableByteChannel) -> FolderEntry
            = { ch -> deserializeFromChannel(ch) }
): FolderItemsStrategy {
    private val initLock = ReentrantLock()

    @Volatile
    private var itemsMap: LinkedHashMap<String, NamedItemDescriptor>? = null
    private var lastEntryOffset: Long = 0L
    override fun containsItem(name: String, cfs: CoreFileSystem, accessor: INodeAccessor)
            : Either<CoreFileSystemError, Boolean> {
        ensureFolderRead(cfs, accessor).mapLeft {
            return it.left()
        }

        return (itemsMap!!.containsKey(name)).right()
    }

    override fun listItems(cfs: CoreFileSystem, accessor: INodeAccessor, lock: Lock)
            : Either<CoreFileSystemError, Iterable<NamedItemDescriptor>> {
        ensureFolderRead(cfs, accessor).mapLeft {
            return it.left()
        }

        lock.withLock {
            val items = ArrayList<NamedItemDescriptor>(itemsMap!!.size)
            itemsMap!!.values.forEach { items.add(it) }
            return items.right()
        }
    }

    override fun getItem(name: String, cfs: CoreFileSystem, accessor: INodeAccessor)
            : Either<CoreFileSystemError, NamedItemDescriptor> {
        ensureFolderRead(cfs, accessor).mapLeft {
            return it.left()
        }

        if(itemsMap!!.containsKey(name)) {
            return itemsMap!![name]!!.right()
        }
        return CoreFileSystemError.ItemNotFoundError(name).left()
    }

    override fun addItem(descriptor: NamedItemDescriptor, cfs: CoreFileSystem, accessor: INodeAccessor)
            : Either<CoreFileSystemError, NamedItemDescriptor> {
        ensureFolderRead(cfs, accessor).mapLeft {
            return it.left()
        }

        itemsMap!![descriptor.name] = descriptor
        val entry = FolderEntry(descriptor.nodeId, descriptor.type.toNodeType, descriptor.name)
        accessor.getSeekableByteChannel(cfs).use { ch ->
            ch.position(lastEntryOffset)
            serialize(ch, entry)
            lastEntryOffset = ch.position()
            serialize(ch, FolderEntry.TerminatingEntry)
        }
        return descriptor.right()
    }

    override fun deleteItem(name: String, cfs: CoreFileSystem, accessor: INodeAccessor)
        : Either<CoreFileSystemError, Unit> {
        ensureFolderRead(cfs, accessor).mapLeft {
            return it.left()
        }

        itemsMap!!.remove(name)
        accessor.getSeekableByteChannel(cfs).use { ch ->
            ch.position(0)
            itemsMap!!.forEach {
                serialize(ch, FolderEntry(it.value.nodeId, it.value.type.toNodeType, it.value.name))
            }
            lastEntryOffset = ch.position()
            serialize(ch, FolderEntry.TerminatingEntry)
            ch.truncate(ch.position())
        }
        return Unit.right()
    }

    private fun ensureFolderRead(coreFileSystem: CoreFileSystem, inodeAccessor: INodeAccessor) : Either<CoreFileSystemError, Unit> {
        if (itemsMap != null) {
            return Unit.right()
        }
        initLock.withLock {
            if(itemsMap != null)
            {
                return Unit.right()
            }

            val map = LinkedHashMap<String, NamedItemDescriptor>()
            inodeAccessor.getSeekableByteChannel(coreFileSystem).use { channel ->
                do {
                    val entry = deserialize(channel)
                    if (entry == FolderEntry.TerminatingEntry) {
                        lastEntryOffset = channel.position() - FolderEntry.TerminatingEntrySize
                        break
                    }
                    coreFileSystem.getInodeItemDescriptor(entry.inodeId).fold(
                            { return it.left() },
                            { map[entry.name] = NamedItemDescriptor(entry.name, it) }
                    )
                } while (true)
            }

            itemsMap = map
        }
        return Unit.right()
    }
}