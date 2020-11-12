package com.mvg.virtualfs.storage

import java.util.*

class BlockGroup (
        val blockSize: Int,
        val numberOfBlocks: Int,
        val numberOfInodes: Int,
        var freeBlocksCount: Int,
        var freeInodesCount: Int,
        var dataBlocksOffset: Long,
        val blockBitMap: BitSet,
        val inodesBitMap: BitSet,
        val inodes: Array<INode>) {

    constructor(_blockSize: Int,
                _numberOfBlocks: Int,
                _numberOfInodes: Int,
                currentBlockOffset: Long,
                firstInodeIndex: Int)
     : this(_blockSize, _numberOfBlocks, _numberOfInodes,
            _numberOfBlocks, _numberOfInodes, 0,
            BitSet(_numberOfBlocks), BitSet(_numberOfInodes),
            Array<INode>(_numberOfInodes) {
                i -> INode(firstInodeIndex + i, NodeType.None, null, null)
            })
    {
        dataBlocksOffset = currentBlockOffset + sizeInBlocks() * blockSize
    }

    fun sizeInBytes(): Int{
        // extra 2 Int.SIZE_BYTES because of collection serialization
        return (5 * Int.SIZE_BYTES + Long.SIZE_BYTES +2*Int.SIZE_BYTES
                + ceilDivide(numberOfBlocks, Byte.SIZE_BITS)
                + ceilDivide(numberOfInodes, Byte.SIZE_BITS)
                + inodes.size * INode.sizeInBytes())
    }

    fun sizeInBlocks(): Int{
        return ceilDivide(sizeInBytes(), blockSize)
    }
}