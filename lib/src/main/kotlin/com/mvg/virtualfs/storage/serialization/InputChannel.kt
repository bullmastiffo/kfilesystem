package com.mvg.virtualfs.storage.serialization

import java.io.Closeable
import java.nio.ByteBuffer
import java.util.*

interface InputChannel : Closeable {
    fun readByteBuffer(size: Int): ByteBuffer
    fun readString(): String
    fun readLong(): Long
    fun readInt(): Int
    fun readByte() : Byte
    fun readDate(): Date?
    fun endOfChannel() : Boolean
}