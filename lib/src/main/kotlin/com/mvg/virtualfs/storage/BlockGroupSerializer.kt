package com.mvg.virtualfs.storage

import com.mvg.virtualfs.storage.serialization.InputChannel
import com.mvg.virtualfs.storage.serialization.OutputChannel
import com.mvg.virtualfs.storage.serialization.OutputChannelSerializer
import com.mvg.virtualfs.storage.serialization.serializeToChannel

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
        TODO("Not yet implemented")
    }
}

object SuperGroupSerializer: OutputChannelSerializer<SuperGroup> {
    override fun serialize(channel: OutputChannel, value: SuperGroup) {
        channel.writeInt(value.totalBlocks)
        channel.writeInt(value.totalInodes)
        channel.writeInt(value.freeBlockCount)
        channel.writeInt(value.freeInodeCount)
        channel.writeInt(value.blockPerGroup)
        channel.writeInt(value.inodesPerGroup)
    }

    override fun deserialize(channel: InputChannel): SuperGroup {
        TODO("Not yet implemented")
    }
}

object INodeSerializer: OutputChannelSerializer<INode> {
    override fun serialize(channel: OutputChannel, value: INode) {
        channel.writeInt(value.id)
        channel.writeByte(value.type.type)
        channel.writeDate(value.created)
        channel.writeDate(value.lastModified)
        value.blockOffsets.forEach { it -> channel.writeLong(it) }
    }

    override fun deserialize(channel: InputChannel): INode {
        TODO("Not yet implemented")
    }
}

object FolderEntrySerializer: OutputChannelSerializer<FolderEntry> {
    override fun serialize(channel: OutputChannel, value: FolderEntry) {
        channel.writeInt(value.inodeId)
        channel.writeByte(value.nodeType.type)
        channel.writeString(value.name)
    }

    override fun deserialize(channel: InputChannel): FolderEntry {
        TODO("Not yet implemented")
    }
}