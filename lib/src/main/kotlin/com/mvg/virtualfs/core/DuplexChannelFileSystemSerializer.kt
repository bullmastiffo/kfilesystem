package com.mvg.virtualfs.core

import java.nio.channels.SeekableByteChannel
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class DuplexChannelFileSystemSerializer(private val channel: SeekableByteChannel) : FileSystemSerializer {
    private val lock = ReentrantLock()
     override fun runSerializationAction(action: (SeekableByteChannel) -> Unit) {
        lock.withLock {
            action(channel)
        }
    }

    override fun close() {
        channel.close()
    }
}