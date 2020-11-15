package com.mvg.virtualfs.storage.serialization

import java.io.Closeable
import java.nio.ByteBuffer
import java.util.*

interface OutputChannel : Closeable {
    fun writeByteBuffer(value: ByteBuffer)
    fun writeString(value: String)
    fun writeLong(value: Long)
    fun writeInt(value: Int)
    fun writeByte(value: Byte)
    fun writeDate(value: Date?)
}