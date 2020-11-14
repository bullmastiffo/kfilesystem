package com.mvg.virtualfs.storage.serialization

import java.nio.ByteBuffer
import java.util.*

interface InputChannel {
    fun readByteBuffer(): ByteBuffer
    fun readString(): String
    fun readLong(): Long
    fun readInt(): Int
    fun readByte() : Byte
    fun readDate(): Date?
}