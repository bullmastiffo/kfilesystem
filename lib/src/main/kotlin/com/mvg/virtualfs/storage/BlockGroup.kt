package com.mvg.virtualfs.storage

import java.util.*

class BlockGroup (
        val numberOfBlocks: Int,
        val numberOfInodes: Int,
        currentBlockOffset: Long,
        val blockSize: Int,
        inodesArray: Array<INode>?,
        firstInodeIndex: Int?) {
    var freeBlocksCount: Int = numberOfBlocks
    var freeInodesCount: Int = numberOfInodes
    val dataBlocksOffset: Long
    val blockBitMap: BitSet
    val inodesBitMap: BitSet
    val inodes: Array<INode>

    init {
        blockBitMap = BitSet(numberOfBlocks)
        inodesBitMap = BitSet(numberOfInodes)

        inodes = inodesArray ?: Array<INode>(numberOfInodes) {
            i -> INode(firstInodeIndex ?: 0 + i, NodeType.None, null, null)
        }
        dataBlocksOffset = currentBlockOffset + sizeInBlocks() * blockSize
    }

    fun sizeInBytes(): Int{
        return (5 * Int.SIZE_BYTES + Long.SIZE_BYTES
                + ceilDivide(numberOfBlocks, Byte.SIZE_BITS)
                + ceilDivide(numberOfInodes, Byte.SIZE_BITS)
                + inodes.size * INode.sizeInBytes())
    }

    fun sizeInBlocks(): Int{
        return ceilDivide(sizeInBytes(), blockSize)
    }
}