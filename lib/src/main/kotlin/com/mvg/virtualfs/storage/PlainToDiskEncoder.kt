package com.mvg.virtualfs.storage

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.channels.WritableByteChannel

@ExperimentalSerializationApi
class PlainToDiskEncoder(private val outputChannel: WritableByteChannel, bufferSize: Int?) : AbstractEncoder(), Closeable {
    private val buffer: ByteArray = ByteArray(bufferSize ?: 1024);
    private var position: Int = 0

    override val serializersModule: SerializersModule = EmptySerializersModule

    override fun encodeNull() {}
    override fun encodeNotNullMark() {}

    //override fun encodeBoolean(value: Boolean)
    //override fun encodeShort(value: Short)
    //override fun encodeFloat(value: Float) = output.writeFloat(value)
    //override fun encodeDouble(value: Double) = output.writeDouble(value)
    //override fun encodeChar(value: Char) = output.writeChar(value.toInt())
    override fun encodeByte(value: Byte) = writeByteToBuffer(value)
    override fun encodeInt(value: Int){
        writeByteToBuffer(((value ushr 24) and 0xFF).toByte())
        writeByteToBuffer(((value ushr 16) and 0xFF).toByte())
        writeByteToBuffer(((value ushr  8) and 0xFF).toByte())
        writeByteToBuffer(((value ushr  0) and 0xFF).toByte())
    }
    override fun encodeLong(value: Long)
    {
        encodeInt(((value ushr 32) and (0.inv()).toLong()).toInt())
        encodeInt((value and (0L.inv() shl 32)).toInt())
    }

    override fun encodeString(value: String)
    {
        var data = value.toByteArray( Charsets.UTF_8)
        encodeInt(data.size)
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

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int)
    {
        writeByteToBuffer(index.toByte())
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        flushToChannel()
    }

    override fun close() {
        flushToChannel()
    }

    private fun writeByteToBuffer(b: Byte){
        buffer[position] = b
        position++
        checkSizeAndFlushOnFull()
    }

    private fun checkSizeAndFlushOnFull(){
        if (position == buffer.size)
        {
            flushToChannel()
        }
    }

    private fun flushToChannel(){
        if(position > 0) {
            outputChannel.write(ByteBuffer.wrap(buffer, 0, position))
            position = 0
        }
    }
}