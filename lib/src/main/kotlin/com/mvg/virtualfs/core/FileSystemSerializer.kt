package com.mvg.virtualfs.core

import java.io.Closeable
import java.nio.channels.SeekableByteChannel

interface FileSystemSerializer : Closeable {
    fun runSerializationAction( action: (SeekableByteChannel) -> Unit)
}