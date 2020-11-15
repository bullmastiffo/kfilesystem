package com.mvg.virtualfs.core

import com.mvg.virtualfs.storage.serialization.DuplexChannel

interface FileSystemSerializer {
    fun runSerializationAction( action: (DuplexChannel) -> Unit)
}