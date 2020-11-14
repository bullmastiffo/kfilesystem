package com.mvg.virtualfs.storage.serialization

interface OutputChannelSerializer<T> {
    fun serialize(channel: OutputChannel, value: T)
    fun deserialize(channel: InputChannel): T
}

