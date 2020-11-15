package com.mvg.virtualfs.storage.serialization

interface DuplexChannel : OutputChannel , InputChannel {
    val position: Long
    fun position(newPosition: Long): DuplexChannel
}