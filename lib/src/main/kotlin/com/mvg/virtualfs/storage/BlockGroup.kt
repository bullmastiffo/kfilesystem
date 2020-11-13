package com.mvg.virtualfs.storage

import kotlinx.serialization.Serializable
import java.util.*

@Serializable(with = BlockGroupSerializer::class)
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
            _numberOfBlocks, _numberOfInodes,
            currentBlockOffset + sizeInBlocks(_blockSize, _numberOfBlocks, _numberOfInodes) * _blockSize,
            BitSet(_numberOfBlocks), BitSet(_numberOfInodes),
            Array<INode>(_numberOfInodes) {
                i -> INode(firstInodeIndex + i, NodeType.None, null, null)
            })
    {
    }
    init {
        blockBitMap[numberOfBlocks-1] = false
        inodesBitMap[numberOfInodes-1] = false
    }

    companion object{
        fun sizeInBytes(numberOfBlocks: Int, numberOfInodes: Int): Int{
            // all fields plus extra 2 Int.SIZE_BYTES because of collection serialization
            return (5 * Int.SIZE_BYTES + Long.SIZE_BYTES + 2 * Int.SIZE_BYTES
                    + ceilDivide(numberOfBlocks, Byte.SIZE_BITS)
                    + ceilDivide(numberOfInodes, Byte.SIZE_BITS)
                    + numberOfInodes * INode.sizeInBytes())
        }

        fun sizeInBlocks(blockSize: Int, numberOfBlocks: Int, numberOfInodes: Int): Int{
            return ceilDivide(sizeInBytes(numberOfBlocks, numberOfInodes), blockSize)
        }
    }
}