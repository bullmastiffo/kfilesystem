package com.mvg.virtualfs

import com.mvg.virtualfs.storage.FixedBitMap
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Tests for @see FixedBitMap.
 * sut - system under test, thus instance of FixedBitMap
 */
internal class FixedBitMapTests {
    private val bitArraySize = 35

    @Test
    fun setAndGetBitWork() {
        val sut = FixedBitMap(bitArraySize)
        for(i in 0 until bitArraySize) {
            sut.setBit(i)
            for(j in 0 until bitArraySize)
                assertEquals(i == j, sut.getBit(j))
            sut.flipBit(i)
            for(j in 0 until bitArraySize)
                assertEquals(false, sut.getBit(j))
        }
    }

    @Test
    fun flipBitChangesGivenBitValue() {
        val sut = FixedBitMap(bitArraySize)
        for(i in 0 until bitArraySize) {
            sut.flipBit(i)
            for(j in 0 until bitArraySize)
                assertEquals(i == j, sut.getBit(j))
            sut.flipBit(i)
            for(j in 0 until bitArraySize)
                assertEquals(false, sut.getBit(j))
        }
    }

    @Test
    fun getHoldingByteAndIndex() {
        val sut = FixedBitMap(bitArraySize)
        val byteOne: Byte = 1
        for(i in 0 until bitArraySize)
        {
            val bitIndex = (i / 8) * Byte.SIZE_BITS
            sut.setBit(bitIndex)
            val (byte, index) = sut.getHoldingByteAndIndex(i)
            assertEquals(i / Byte.SIZE_BITS, index)
            assertEquals(byteOne, byte)
            sut.flipBit(bitIndex)
        }
    }

    @Test
    fun nextClearBitReturnsNextClearBit() {
        val sut = FixedBitMap(bitArraySize)
        for(i in 0 until bitArraySize)
        {
            val nextClear = sut.nextClearBit()
            assertEquals(i, nextClear)
            sut.flipBit(i)
        }
    }

    @Test
    fun nextClearBitReturnsMinusOneOnFull() {
        val roundBytesSize = 32
        val sut = FixedBitMap(roundBytesSize)
        for(i in 0 until roundBytesSize)
        {
            sut.setBit(i)
        }
        val nextClear = sut.nextClearBit()
        assertEquals(-1, nextClear)
    }
}