package com.mvg.virtualfs.storage.serialization

import java.io.Closeable
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel
import java.util.*

class NioDuplexChannel (
        private val channel: SeekableByteChannel,
        private val closeUnderlying: Boolean = false): DuplexChannel, Closeable {
    private val buffer: ByteArray = ByteArray(1024)
    private var bufferPosition: Int = 0
    override val position: Long
        get() = channel.position()

    override fun position(newPosition: Long): DuplexChannel {
        channel.position(newPosition)
        return this
    }

    override fun writeByteBuffer(value: ByteBuffer) {
        flushToChannel()
        channel.write(value)
    }

    override fun writeString(value: String) {
        val data = value.toByteArray( Charsets.UTF_8)
        writeInt(data.size)
        var delta = buffer.size - bufferPosition
        var srcPosition = 0
        var minWrite = if (data.size > delta) delta else data.size
        while(minWrite > 0) {
            data.copyInto(buffer, bufferPosition,  srcPosition, minWrite)
            bufferPosition+=minWrite
            srcPosition+=minWrite
            checkSizeAndFlushOnFull()
            minWrite = data.size - srcPosition
            delta = buffer.size - bufferPosition
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
        buffer[bufferPosition] = value
        bufferPosition++
        checkSizeAndFlushOnFull()
    }

    override fun writeDate(value: Date?) {
        writeLong(value?.time ?: 0L)
    }


    private fun checkSizeAndFlushOnFull(){
        if (bufferPosition == buffer.size)
        {
            flushToChannel()
        }
    }

    private fun flushToChannel(){
        if(bufferPosition > 0) {
            channel.write(ByteBuffer.wrap(buffer, 0, bufferPosition))
            bufferPosition = 0
        }
    }

    override fun close() {
        flushToChannel()
        if(closeUnderlying) {
            channel.close()
        }
    }

    override fun readByteBuffer(size: Int): ByteBuffer {
        val buffer = ByteBuffer.allocate(size)
        val read = channel.read(buffer)
        if(read != size)
            throw IOException("Expected to read $size, but read $read")
        return buffer
    }

    override fun readString(): String {
        val dataSize = readInt()
        val buffer = readByteBuffer(dataSize)
        return buffer.array().toString(Charsets.UTF_8)
    }

    override fun readLong(): Long {
        val buffer = readByteBuffer(Long.SIZE_BYTES)
        var result = 0L

        buffer.array().forEach { result = (result or it.toLong()) shl 8 }
        return result
    }

    override fun readInt(): Int {
        val buffer = readByteBuffer(Int.SIZE_BYTES)
        var result = 0

        buffer.array().forEach { result = (result or it.toInt()) shl 8 }
        return result
    }

    override fun readByte(): Byte {
        val buffer = readByteBuffer(1)
        return buffer[0]
    }

    override fun readDate(): Date? {
        val lDate = readLong()
        return if(lDate == 0L) { null } else { Date(lDate) }
    }

    override fun endOfChannel(): Boolean {
        return channel.position() == channel.size()
    }
}