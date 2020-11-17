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

    fun getHoldingByteAndIndex(n: Int): Pair<Byte, Int>
    {
        val (byteIndex, _) = checkAndGetIndexes(n)
        return Pair(byteArray[byteIndex], byteIndex)
    }

    fun nextClearBit(): Int {
        var byteIndex = 0
        while(byteArray[byteIndex] == FILLED_BYTE && byteIndex < byteArray.size) {
            byteIndex++
        }

        if(byteIndex == byteArray.size){
            return -1
        }
        var b = byteArray[byteIndex].toInt() and 0xFF
        var i = 0
        while (b and (1 shl i) > 0){
            i++
        }

        return byteIndex * Byte.SIZE_BITS + i
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
        const val FILLED_BYTE: Byte = -1
        fun getBytesSize(size: Int) = ceilDivide(size, Byte.SIZE_BITS)
    }
}
