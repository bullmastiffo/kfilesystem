package com.mvg.virtualfs.storage

import java.util.*

class BlockGroup (
        numberOfBlocks: Int,
        numberOfInodes: Int,
        val dataBlocksOffset: Long,
        blSize: Int?) {
    val blockSize: Int
    var freeBlocksCount: Int
    val blockBitMap: BitSet
    var freeInodesCount: Int
    val inodesBitMap: BitSet

    init {
        if (blSize != null && blSize !in 0x400..0x8000){
            throw IllegalArgumentException("blSize must be between 1kb and 32kb")
        }
        blockSize = blSize ?: SIZE_4KB
        if (numberOfBlocks <= 0)
        {
            throw IllegalArgumentException("numberOfBlocks must be greater then 0")
        }
        freeBlocksCount = numberOfBlocks
        blockBitMap = BitSet(numberOfBlocks)
        if (numberOfInodes <= 0)
        {
            throw IllegalArgumentException("numberOfInodes must be greater then 0")
        }
        freeInodesCount = numberOfInodes
        inodesBitMap = BitSet(numberOfInodes)
    }

    companion object {
        const val SIZE_4KB = 0x1000
    }
}