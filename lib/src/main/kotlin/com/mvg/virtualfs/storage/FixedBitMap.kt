package com.mvg.virtualfs.storage

/**
 *  Instantiates bit map with given byte array. Array is not copied!
 * @property byteArray ByteArray
 * @constructor
 */
class FixedBitMap(private val byteArray: ByteArray) {

    /**
     * Instantiates bit map with size to at least hold size bits
     * (actual capacity can be larger, rounded to larger amount of bytes to hold all the bits.
     * @param size Int
     * @constructor
     */
    constructor(size: Int) : this(ByteArray(ceilDivide(size, Byte.SIZE_BITS))){}

    /**
     * Gets given bit value
     * @param n Int
     * @return Boolean
     */
    fun getBit(n: Int): Boolean
    {
        val (byteIndex, bitIndex) = checkAndGetIndexes(n)
        return (byteArray[byteIndex].toInt() and (1 shl bitIndex)) != 0
    }

    /**
     * Sets given bit to true / 1.
     * @param n Int
     */
    fun setBit(n: Int)
    {
        val (byteIndex, bitIndex) = checkAndGetIndexes(n)

        var b = byteArray[byteIndex].toInt()
        b = b or (1 shl bitIndex)
        byteArray[byteIndex] = b.toByte()
    }

    /**
     * Flips bit at given index.
     * @param n Int
     */
    fun flipBit(n: Int)
    {
        val (byteIndex, bitIndex) = checkAndGetIndexes(n)

        var b = byteArray[byteIndex].toInt()
        b = b xor (1 shl bitIndex)
        byteArray[byteIndex] = b.toByte()
    }

    /**
     * Gets byte value for given bit and index of byte in the byte array.
     * @param n Int
     * @return Pair<Byte, Int>
     */
    fun getHoldingByteAndIndex(n: Int): Pair<Byte, Int>
    {
        val (byteIndex, _) = checkAndGetIndexes(n)
        return Pair(byteArray[byteIndex], byteIndex)
    }

    /**
     * Returns next clear bit in bitmap or -1 if Bitmap is full.
     * @return Int
     */
    fun nextClearBit(): Int {
        var byteIndex = 0
        while(byteIndex < byteArray.size && byteArray[byteIndex] == FILLED_BYTE) {
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
        val clearIndex = byteIndex * Byte.SIZE_BITS + i
        if(i >= Byte.SIZE_BITS){
            return -1
        }

        return clearIndex
    }

    private fun checkAndGetIndexes(n: Int): Pair<Int, Int>
    {
        val byteIndex = n / Byte.SIZE_BITS
        if(byteIndex >= byteArray.size){
            throw IndexOutOfBoundsException()
        }
        val bitIndex = n % Byte.SIZE_BITS
        return Pair(byteIndex, bitIndex)
    }

    /**
     * Returns underlying byte array without copying
     * @return ByteArray
     */
    internal fun toByteArray(): ByteArray = byteArray

    companion object{
        const val FILLED_BYTE: Byte = -1
        fun getBytesSize(size: Int) = ceilDivide(size, Byte.SIZE_BITS)
    }
}
