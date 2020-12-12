package com.mvg.virtualfs.core

import arrow.core.Either
import java.nio.channels.SeekableByteChannel

abstract class FileHandlerDecoratorBase(private val target: FileHandler): FileHandler {
    override val size: Long
        get() = target.size

    override fun getByteChannel(): Either<CoreFileSystemError, SeekableByteChannel> {
        return target.getByteChannel()
    }

    override val descriptor: NamedItemDescriptor
        get() = target.descriptor

    override fun close() {
        target.close()
    }

    override fun delete(): Either<CoreFileSystemError, Unit> {
        return target.delete()
    }
}