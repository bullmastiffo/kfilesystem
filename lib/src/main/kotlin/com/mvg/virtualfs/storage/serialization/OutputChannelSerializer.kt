package com.mvg.virtualfs.storage.serialization

import java.nio.channels.ReadableByteChannel
import java.nio.channels.WritableByteChannel

interface OutputChannelSerializer<T> {
    fun serialize(channel: WritableByteChannel, value: T)
    fun deserialize(channel: ReadableByteChannel): T
}

