package com.mvg.virtualfs.core

import com.mvg.virtualfs.storage.serialization.DuplexChannel
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class DuplexChannelFileSystemSerializer(private val channel: DuplexChannel) : FileSystemSerializer {
    private val lock = ReentrantLock()
     override fun runSerializationAction(action: (DuplexChannel) -> Unit) {
        lock.withLock {
            action(channel)
        }
    }

    override fun close() {
        channel.close()
    }
}