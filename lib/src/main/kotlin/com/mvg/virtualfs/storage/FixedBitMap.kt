package com.mvg.virtualfs.storage

import java.nio.ByteBuffer

class FixedBitMap(private val byteArray: ByteArray) {

    constructor(size: Int) : this(ByteArray(ceilDivide(size, Byte.SIZE_BITS))){}

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
        val byteIndex = ceilDivide(n, Byte.SIZE_BITS)
        if(byteIndex >= byteArray.size){
            throw IndexOutOfBoundsException()
        }
        val bitIndex = n % Byte.SIZE_BITS
        return Pair(byteIndex, bitIndex)
    }

    internal fun toByteBuffer(): ByteBuffer = ByteBuffer.wrap(byteArray)

    companion object{
        fun getBytesSize(size: Int) = ceilDivide(size, Byte.SIZE_BITS)
    }
}
