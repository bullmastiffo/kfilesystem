package com.mvg.virtualfs.core

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import java.nio.channels.SeekableByteChannel
import java.util.concurrent.atomic.AtomicBoolean

class ActiveFileHandler(
        private val inodeAccessor: INodeAccessor,
        private val coreFileSystem: CoreFileSystem,
        override val descriptor: NamedItemDescriptor): FileHandler{

    private var isClosed = AtomicBoolean(false)
    override val size: Long
        get() = inodeAccessor.size

    override fun getStream(): Either<CoreFileSystemError, SeekableByteChannel> {
        if (isClosed.get()){
            return CoreFileSystemError.ItemClosedError.left()
        }
        return inodeAccessor.getSeekableByteChannel(coreFileSystem).right()
    }

    override fun close() {
        if(!isClosed.getAndSet(true)) {
            inodeAccessor.close()
        }
    }

}