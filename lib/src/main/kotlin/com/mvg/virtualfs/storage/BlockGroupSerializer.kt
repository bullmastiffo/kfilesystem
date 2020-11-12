package com.mvg.virtualfs.storage

import kotlinx.serialization.*
import kotlinx.serialization.builtins.ArraySerializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import java.util.*

private val byteArraySerializer = serializer<ByteArray>()
private val inodeArraySerializer = ArraySerializer(serializer<INode>())
object BlockGroupSerializer: KSerializer<BlockGroup> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("BlockGroup")
    {
        element<Int>("blockSize")
        element<Int>("numberOfBlocks")
        element<Int>("numberOfInodes")
        element<Long>("currentBlockOffset")
        element<Int>("freeBlocksCount")
        element<Int>("freeInodesCount")
        element<ByteArray>("blockBitMap")
        element<ByteArray>("inodesBitMap")
        element<Array<INode>>("inodesArray")
    }

    override fun serialize(encoder: Encoder, value: BlockGroup) {
        encoder.encodeStructure(descriptor)
        {
            encodeIntElement(descriptor, 0,value.blockSize)
            encodeIntElement(descriptor, 1, value.numberOfBlocks)
            encodeIntElement(descriptor, 2,value.numberOfInodes)
            encodeLongElement(descriptor, 3,value.dataBlocksOffset)
            encodeIntElement(descriptor, 4,value.freeBlocksCount)
            encodeIntElement(descriptor, 5,value.freeInodesCount)
            encodeSerializableElement<ByteArray>(descriptor, 6, byteArraySerializer, value.blockBitMap.toByteArray())
            encodeSerializableElement<ByteArray>(descriptor, 7, byteArraySerializer, value.inodesBitMap.toByteArray())
            encodeSerializableElement<Array<INode>>(descriptor, 8, inodeArraySerializer, value.inodes)
        }
    }

    override fun deserialize(decoder: Decoder): BlockGroup {
        return decoder.decodeStructure(descriptor) {
            var blockSize: Int = 0
            var numberOfBlocks: Int = 0
            var numberOfInodes: Int = 0
            var inodesArray: Array<INode>
            var freeBlocksCount: Int = 0
            var freeInodesCount: Int = 0
            var dataBlocksOffset: Long = 0
            var blockBitMap: BitSet? = null
            var inodesBitMap: BitSet? = null
            var inodes: Array<INode>? = null

            if (decodeSequentially()) { // sequential decoding protocol
                blockSize = decodeIntElement(descriptor, 0)
                numberOfBlocks = decodeIntElement(descriptor, 1)
                numberOfInodes = decodeIntElement(descriptor, 2)
                dataBlocksOffset = decodeLongElement(descriptor, 3)
                freeBlocksCount = decodeIntElement(descriptor, 4)
                freeInodesCount = decodeIntElement(descriptor, 5)
                var blockBitArray = decodeSerializableElement(descriptor, 6, byteArraySerializer, null)
                blockBitMap = BitSet.valueOf(blockBitArray)
                var inodesBitArray = decodeSerializableElement(descriptor, 7, byteArraySerializer, null)
                inodesBitMap = BitSet.valueOf(inodesBitArray)
                inodes = decodeSerializableElement(descriptor, 8, inodeArraySerializer, null)
            } else {
                var index: Int = decodeElementIndex(descriptor)
                while (index != CompositeDecoder.DECODE_DONE) {
                    when (index) {
                        0 -> blockSize = decodeIntElement(descriptor, 0)
                        1 -> numberOfBlocks = decodeIntElement(descriptor, 1)
                        2 -> numberOfInodes = decodeIntElement(descriptor, 2)
                        3 -> dataBlocksOffset = decodeLongElement(descriptor, 3)
                        4 -> freeBlocksCount = decodeIntElement(descriptor, 4)
                        5 -> freeInodesCount = decodeIntElement(descriptor, 5)
                        6 -> blockBitMap = BitSet.valueOf(
                                decodeSerializableElement(descriptor, 6, byteArraySerializer, null))
                        7 -> inodesBitMap = BitSet.valueOf(
                                decodeSerializableElement(descriptor, 7, byteArraySerializer, null))
                        8 -> inodes = decodeSerializableElement(descriptor, 8, inodeArraySerializer, null)
                        else -> error("Unexpected index: $index")
                    }
                    index = decodeElementIndex(descriptor)
                }
            }
            BlockGroup(blockSize, numberOfBlocks, numberOfInodes, freeBlocksCount, freeInodesCount,
                    dataBlocksOffset, blockBitMap!!, inodesBitMap!!, inodes!!)
        }
    }
}