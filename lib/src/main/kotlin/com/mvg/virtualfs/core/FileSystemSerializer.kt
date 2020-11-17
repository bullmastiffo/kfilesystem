package com.mvg.virtualfs.core

import com.mvg.virtualfs.storage.serialization.DuplexChannel
import java.io.Closeable

interface FileSystemSerializer : Closeable {
    fun runSerializationAction( action: (DuplexChannel) -> Unit)
}