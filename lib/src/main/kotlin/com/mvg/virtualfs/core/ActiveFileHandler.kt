package com.mvg.virtualfs.core

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import java.nio.channels.SeekableByteChannel
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock

class ActiveFileHandler(
        private val inodeAccessor: INodeAccessor,
        private val coreFileSystem: CoreFileSystem,
        override val descriptor: NamedItemDescriptor): FileHandler{

    private val lock = ReentrantLock()
    private var channel: SeekableByteChannel? = null
    private val isClosed = AtomicBoolean(false)
    override val size: Long
        get() = inodeAccessor.size

    override fun getByteChannel(): Either<CoreFileSystemError, SeekableByteChannel> {
        if (isClosed.get()){
            return CoreFileSystemError.ItemClosedError.left()
        }

        if(!lock.tryLock()){
            return CoreFileSystemError.ItemAlreadyOpenedError.left()
        }
        channel = inodeAccessor.getSeekableByteChannel(coreFileSystem) { lock.unlock() }
        return channel!!.right()
    }

    override fun delete(): Either<CoreFileSystemError, Unit> {
        return getByteChannel().flatMap {
            it.truncate(0L)
            coreFileSystem.deleteItem(inodeAccessor)
        }
    }

    override fun close() {
        if(!isClosed.getAndSet(true)) {
            channel?.close()
        }
    }
}