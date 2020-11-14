package com.mvg.virtualfs.storage.serialization

import java.nio.ByteBuffer
import java.util.*

interface OutputChannel {
    fun writeByteBuffer(value: ByteBuffer)
    fun writeString(value: String)
    fun writeLong(value: Long)
    fun writeInt(value: Int)
    fun writeByte(value: Byte)
    fun writeDate(value: Date?)
}