package com.mvg.virtualfs.storage.serialization

import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel
import java.util.*

class SeekableChannel (val channel: SeekableByteChannel): OutputChannel , Closeable {
    private val buffer: ByteArray = ByteArray(1024);
    private var position: Int = 0

    override fun writeByteBuffer(value: ByteBuffer) {
        flushToChannel()
        channel.write(value)
    }

    override fun writeString(value: String) {
        var data = value.toByteArray( Charsets.UTF_8)
        writeInt(data.size)
        var delta = buffer.size - position
        var srcPosition = 0
        var minWrite = if (data.size > delta) delta else data.size
        while(minWrite > 0) {
            data.copyInto(buffer, position,  srcPosition, minWrite)
            position+=minWrite
            srcPosition+=minWrite
            checkSizeAndFlushOnFull()
            minWrite = data.size - srcPosition
            delta = buffer.size - position
            minWrite = if (minWrite > delta) delta else minWrite
        }
    }

    override fun writeLong(value: Long) {
        writeInt(((value ushr 32) and (0.inv()).toLong()).toInt())
        writeInt((value and (0L.inv() shl 32)).toInt())
    }

    override fun writeInt(value: Int) {
        writeByte(((value ushr 24) and 0xFF).toByte())
        writeByte(((value ushr 16) and 0xFF).toByte())
        writeByte(((value ushr  8) and 0xFF).toByte())
        writeByte(((value ushr  0) and 0xFF).toByte())
    }

    override fun writeByte(value: Byte) {
        buffer[position] = value
        position++
        checkSizeAndFlushOnFull()
    }

    override fun writeDate(value: Date?) {
        writeLong(value?.time ?: 0L)
    }


    private fun checkSizeAndFlushOnFull(){
        if (position == buffer.size)
        {
            flushToChannel()
        }
    }

    private fun flushToChannel(){
        if(position > 0) {
            channel.write(ByteBuffer.wrap(buffer, 0, position))
            position = 0
        }
    }

    override fun close() {
        flushToChannel()
    }
}