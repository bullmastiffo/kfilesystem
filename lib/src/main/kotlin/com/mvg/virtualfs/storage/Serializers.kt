package com.mvg.virtualfs.storage

import com.mvg.virtualfs.core.NodeType
import com.mvg.virtualfs.storage.serialization.*

object BlockGroupSerializer: OutputChannelSerializer<BlockGroup> {
    override fun serialize(channel: OutputChannel, value: BlockGroup) {
        channel.writeInt(value.blockSize)
        channel.writeInt(value.numberOfBlocks)
        channel.writeInt(value.numberOfInodes)
        channel.writeLong(value.dataBlocksOffset)
        channel.writeInt(value.freeBlocksCount)
        channel.writeInt(value.freeInodesCount)
        channel.writeByteBuffer(value.blockBitMap.toByteBuffer())
        channel.writeByteBuffer(value.inodesBitMap.toByteBuffer())
        value.inodes.forEach { serializeToChannel(channel, it) }
    }

    override fun deserialize(channel: InputChannel): BlockGroup {
        val blockSize = channel.readInt()
        val numberOfBlocks = channel.readInt()
        val numberOfInodes = channel.readInt()
        val dataBlocksOffset = channel.readLong()
        val freeBlocksCount = channel.readInt()
        val freeInodesCount = channel.readInt()
        var buffer = channel.readByteBuffer(FixedBitMap.getBytesSize(numberOfBlocks))
        val blockBitMap = FixedBitMap(buffer.array())
        buffer = channel.readByteBuffer(FixedBitMap.getBytesSize(numberOfInodes))
        val inodesBitMap = FixedBitMap(buffer.array())
        val inodes = Array<INode>(numberOfInodes) { deserializeFromChannel(channel) }
        
        return BlockGroup(blockSize, numberOfBlocks, numberOfInodes, freeBlocksCount, freeInodesCount, dataBlocksOffset, blockBitMap, inodesBitMap, inodes)
    }
}

object SuperGroupSerializer: OutputChannelSerializer<SuperGroup> {
    override fun serialize(channel: OutputChannel, value: SuperGroup) {
        channel.writeInt(value.blockSize)
        channel.writeInt(value.totalBlocks)
        channel.writeInt(value.totalInodes)
        channel.writeInt(value.totalBlockGroups)
        channel.writeInt(value.freeBlockCount)
        channel.writeInt(value.freeInodeCount)
        channel.writeInt(value.blockPerGroup)
        channel.writeInt(value.inodesPerGroup)
    }

    override fun deserialize(channel: InputChannel): SuperGroup {
        val blockSize = channel.readInt()
        val totalBlocks = channel.readInt()
        val totalInodes = channel.readInt()
        val totalBlockGroups = channel.readInt()
        val freeBlockCount = channel.readInt()
        val freeInodeCount = channel.readInt()
        val blockPerGroup = channel.readInt()
        val inodesPerGroup = channel.readInt()

        return SuperGroup(blockSize, totalBlocks, totalInodes, totalBlockGroups, freeBlockCount, freeInodeCount, blockPerGroup, inodesPerGroup)
    }
}

object INodeSerializer: OutputChannelSerializer<INode> {
    override fun serialize(channel: OutputChannel, value: INode) {
        channel.writeInt(value.id)
        channel.writeByte(value.type.type)
        channel.writeDate(value.created)
        channel.writeDate(value.lastModified)
        value.blockOffsets.forEach { channel.writeLong(it) }
    }

    override fun deserialize(channel: InputChannel): INode {
        val id = channel.readInt()
        val type = channel.readByte()
        val created = channel.readDate()
        val lm = channel.readDate()
        val offsets = LongArray(INode.OFFSETS_SIZE)
        for(i in 0 until INode.OFFSETS_SIZE)
            offsets[i] = channel.readLong()

        return INode(id, NodeType.fromByte(type), created, lm, offsets)
    }
}

object FolderEntrySerializer: OutputChannelSerializer<FolderEntry> {
    override fun serialize(channel: OutputChannel, value: FolderEntry) {
        channel.writeInt(value.inodeId)
        channel.writeByte(value.nodeType.type)
        channel.writeString(value.name)
    }

    override fun deserialize(channel: InputChannel): FolderEntry {
        val inodeId = channel.readInt()
        val nodeType = NodeType.fromByte(channel.readByte())
        val name = channel.readString()
        return FolderEntry(inodeId, nodeType, name)
    }
}