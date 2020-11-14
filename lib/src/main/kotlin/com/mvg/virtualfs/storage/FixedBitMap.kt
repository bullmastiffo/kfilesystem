package com.mvg.virtualfs.storage

import java.nio.ByteBuffer

class FixedBitMap(val size: Int) {
    private val byteArray: ByteArray

    init {
        val byteSize = ceilDivide(size, Byte.SIZE_BITS)
        byteArray = ByteArray(byteSize)
    }

    fun getBit(n: Int): Boolean
    {
        val (byteIndex, bitIndex) = checkAndGetIndexes(n)
        return (byteArray[byteIndex].toInt() and (1 shl bitIndex)) != 0
    }

    fun setBit(n: Int)
    {
        val (byteIndex, bitIndex) = checkAndGetIndexes(n)

        var b = byteArray[byteIndex].toInt()
        b = b or (1 shl bitIndex)
        byteArray[byteIndex] = b.toByte()
    }

    fun flipBit(n: Int)
    {
        val (byteIndex, bitIndex) = checkAndGetIndexes(n)

        var b = byteArray[byteIndex].toInt()
        b = b xor (1 shl bitIndex)
        byteArray[byteIndex] = b.toByte()
    }

    private fun checkAndGetIndexes(n: Int): Pair<Int, Int>
    {
        if(n >= size){
            throw IndexOutOfBoundsException()
        }
        val byteIndex = ceilDivide(n, Byte.SIZE_BITS)
        val bitIndex = n % Byte.SIZE_BITS
        return Pair(byteIndex, bitIndex)
    }

    fun toByteBuffer(): ByteBuffer = ByteBuffer.wrap(byteArray)
}
