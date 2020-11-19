package com.mvg.virtualfs.storage

import com.mvg.virtualfs.core.NodeType
import com.mvg.virtualfs.storage.serialization.OutputChannelSerializable

@OutputChannelSerializable(with = BlockGroupSerializer::class)
class BlockGroup (
        val blockSize: Int,
        val numberOfBlocks: Int,
        val numberOfInodes: Int,
        var freeBlocksCount: Int,
        var freeInodesCount: Int,
        var dataBlocksOffset: Long,
        val blockBitMap: FixedBitMap,
        val inodesBitMap: FixedBitMap,
        val inodes: Array<INode>) {

    val startBlockOffset = dataBlocksOffset - sizeInBlocks(blockSize, numberOfBlocks, numberOfInodes) * blockSize
    val freeBlocksCountOffset = startBlockOffset + 3 * Int.SIZE_BYTES
    val freeInodesCountOffset = startBlockOffset + 4 * Int.SIZE_BYTES
    val bitmapBlocksOffset = startBlockOffset +  5 * Int.SIZE_BYTES + Long.SIZE_BYTES
    val bitmapInodesOffset = bitmapBlocksOffset + ceilDivide(numberOfBlocks, Byte.SIZE_BITS)
    val inodesOffset = bitmapInodesOffset + ceilDivide(numberOfInodes, Byte.SIZE_BITS)

    constructor(_blockSize: Int,
                _numberOfBlocks: Int,
                _numberOfInodes: Int,
                currentBlockOffset: Long,
                firstInodeIndex: Int)
     : this(_blockSize, _numberOfBlocks, _numberOfInodes,
            _numberOfBlocks, _numberOfInodes,
            currentBlockOffset + sizeInBlocks(_blockSize, _numberOfBlocks, _numberOfInodes) * _blockSize,
            FixedBitMap(_numberOfBlocks), FixedBitMap(_numberOfInodes),
            Array<INode>(_numberOfInodes) {
                i -> INode(firstInodeIndex + i, NodeType.None, null, null)
            })
    {
    }

    fun size() : Int{
        return sizeInBytes(numberOfBlocks, numberOfInodes)
    }

    companion object{
        fun sizeInBytes(numberOfBlocks: Int, numberOfInodes: Int): Int{
            return (5 * Int.SIZE_BYTES + Long.SIZE_BYTES
                    + ceilDivide(numberOfBlocks, Byte.SIZE_BITS)
                    + ceilDivide(numberOfInodes, Byte.SIZE_BITS)
                    + numberOfInodes * INode.sizeInBytes())
        }

        fun sizeInBlocks(blockSize: Int, numberOfBlocks: Int, numberOfInodes: Int): Int{
            return ceilDivide(sizeInBytes(numberOfBlocks, numberOfInodes), blockSize)
        }
    }
}