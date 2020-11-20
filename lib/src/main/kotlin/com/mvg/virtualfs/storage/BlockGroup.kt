package com.mvg.virtualfs.storage

import com.mvg.virtualfs.core.NodeType
import com.mvg.virtualfs.storage.serialization.OutputChannelSerializable

/**
 * Represents descriptor of block group in virtual filesystem
 * @property blockSize Int File system block size
 * @property numberOfBlocks Int Total number of data blocks in block group
 * @property numberOfInodes Int Total number of inodes in block group
 * @property freeBlocksCount Int Number of free data blocks in block group
 * @property freeInodesCount Int Number of free inodes in block group
 * @property dataBlocksOffset Long Offset of first data block in containing channel
 * @property blockBitMap FixedBitMap Bit map of occupied data blocks
 * @property inodesBitMap FixedBitMap Bit map of occupied inodes
 * @property inodes Array<INode> Inodes descriptors arrays.
 * @property startBlockOffset Long Start offset of this block group in containing channel
 * @property freeBlocksCountOffset Long Offset of free blocks counter in containing channel
 * @property freeInodesCountOffset Long Offset of free inodes counter in containing channel
 * @property bitmapBlocksOffset Long Offset of block bitmap in containing channel
 * @property bitmapInodesOffset Long Offset of inodes bitmap in containing channel
 * @property inodesOffset Long Offset of inodes array in containing channel
 * @constructor
 */
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