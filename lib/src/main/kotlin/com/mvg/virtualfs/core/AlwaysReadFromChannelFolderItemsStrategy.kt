package com.mvg.virtualfs.core

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.mvg.virtualfs.storage.FolderEntry
import com.mvg.virtualfs.storage.serialization.deserializeFromChannel
import com.mvg.virtualfs.storage.serialization.serializeToChannel
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel
import java.util.concurrent.locks.Lock

class AlwaysReadFromChannelFolderItemsStrategy : FolderItemsStrategy {
    override fun containsItem(name: String, cfs: CoreFileSystem, accessor: INodeAccessor)
        : Either<CoreFileSystemError, Boolean> {
        return getItem(name, cfs, accessor).fold(
                {
                    when(it){
                        is CoreFileSystemError.ItemNotFoundError -> false.right()
                        else -> it.left()
                    }
                },
                {
                    true.right()
                })
    }

    override fun listItems(cfs: CoreFileSystem, accessor: INodeAccessor, lock: Lock): Either<CoreFileSystemError, Iterable<NamedItemDescriptor>> {
        return ItemsReadIterable(cfs, accessor, lock).right()
    }

    override fun getItem(name: String, cfs: CoreFileSystem, accessor: INodeAccessor)
        : Either<CoreFileSystemError, NamedItemDescriptor> {
        accessor.getSeekableByteChannel(cfs).use { channel ->
            do {
                val entry = deserializeFromChannel<FolderEntry>(channel)
                if (entry.name == name) {
                    return cfs.getInodeItemDescriptor(entry.inodeId).flatMap {
                        NamedItemDescriptor(entry.name, it).right()
                    }
                }
                if (entry == FolderEntry.TerminatingEntry) {
                    return CoreFileSystemError.ItemNotFoundError(name).left()
                }
            } while (true)
        }
    }

    override fun addItem(descriptor: NamedItemDescriptor, cfs: CoreFileSystem, accessor: INodeAccessor): Either<CoreFileSystemError, NamedItemDescriptor> {
        val entry = FolderEntry(descriptor.nodeId, descriptor.type.toNodeType, descriptor.name)
        val lastEntryOffset = accessor.size - FolderEntry.TerminatingEntrySize
        accessor.getSeekableByteChannel(cfs).use { ch ->
            ch.position(lastEntryOffset)
            serializeToChannel(ch, entry)
            serializeToChannel(ch, FolderEntry.TerminatingEntry)
        }
        return descriptor.right()
    }

    override fun deleteItem(name: String, cfs: CoreFileSystem, accessor: INodeAccessor): Either<CoreFileSystemError, Unit> {
        accessor.getSeekableByteChannel(cfs).use { channel ->
            var itemToDeleteOffset = 0L
            var found = false
            while (!found) {
                itemToDeleteOffset = channel.position()
                val entry = deserializeFromChannel<FolderEntry>(channel)
                if (entry.name == name) {
                    found = true
                }
                if (entry == FolderEntry.TerminatingEntry) {
                    return CoreFileSystemError.ItemNotFoundError(name).left()
                }
            }

            val buffer = ByteBuffer.allocate(cfs.fileSystemInfo.blockSize)
            val positionDelta = channel.position() - itemToDeleteOffset
            var read = channel.read(buffer)
            while (read > 0) {
                channel.position(channel.position() - read - positionDelta)
                buffer.flip()
                channel.write(buffer)
                channel.position(channel.position() + positionDelta)
                buffer.clear()
                read = channel.read(buffer)
            }
            channel.truncate(channel.position() - positionDelta)
        }
        return Unit.right()
    }
}

private class ItemsReadIterable(
        val coreFileSystem: CoreFileSystem,
        private val accessor: INodeAccessor,
        private val lock: Lock) : Iterable<NamedItemDescriptor> {
    override fun iterator(): Iterator<NamedItemDescriptor> {
        return ItemsIterator(coreFileSystem, accessor.getSeekableByteChannel(coreFileSystem) { lock.unlock() })
    }

    class ItemsIterator (
            private val coreFileSystem: CoreFileSystem,
            private val channel: SeekableByteChannel
            ): Iterator<NamedItemDescriptor> {

        private var nextEntry: FolderEntry? = null

        override fun hasNext(): Boolean {
            readNextEntry()
            return nextEntry != FolderEntry.TerminatingEntry
        }

        override fun next(): NamedItemDescriptor {
            readNextEntry()
            val entry = nextEntry!!
            nextEntry = null
            if (entry == FolderEntry.TerminatingEntry){
                throw NoSuchElementException()
            }
            return coreFileSystem.getInodeItemDescriptor(entry.inodeId).fold(
                    { throw IOException("Error getting entry descriptor: $it") },
                    { NamedItemDescriptor(entry.name, it) })
        }

        private fun readNextEntry() {
            if (nextEntry == null) {
                nextEntry = deserializeFromChannel<FolderEntry>(channel)
                closeChannelIfLastEntry()
            }
        }

        private fun closeChannelIfLastEntry() {
            if (nextEntry == FolderEntry.TerminatingEntry) {
                channel.close()
            }
        }

        protected fun finalize() {
            channel.close()
        }
    }
}