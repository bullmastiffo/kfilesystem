package com.mvg.virtualfs.storage

import com.mvg.virtualfs.core.NodeType
import com.mvg.virtualfs.storage.serialization.*
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel
import java.nio.channels.WritableByteChannel
import java.util.*

object BlockGroupSerializer: OutputChannelSerializer<BlockGroup> {
    override fun serialize(channel: WritableByteChannel, value: BlockGroup) {
        var buffer = ByteBuffer.allocate(value.size() - value.numberOfInodes * INode.sizeInBytes())
        .putInt(value.blockSize)
        .putInt(value.numberOfBlocks)
        .putInt(value.numberOfInodes)
        .putInt(value.freeBlocksCount)
        .putInt(value.freeInodesCount)
        .putLong(value.dataBlocksOffset)
        .put(value.blockBitMap.toByteArray())
        .put(value.inodesBitMap.toByteArray())
        channel.write(buffer.flip())
        value.inodes.forEach { serializeToChannel(channel, it) }
    }

    override fun deserialize(channel: ReadableByteChannel): BlockGroup {
        var buffer = channel.readBuffer(5 * Int.SIZE_BYTES + Long.SIZE_BYTES, "BlockGroup")
        val blockSize = buffer.getInt()
        val numberOfBlocks = buffer.getInt()
        val numberOfInodes = buffer.getInt()
        val freeBlocksCount = buffer.getInt()
        val freeInodesCount = buffer.getInt()
        val dataBlocksOffset = buffer.getLong()

        buffer = channel.readBuffer(FixedBitMap.getBytesSize(numberOfBlocks), "BlockGroup.blockBitMap")
        val blockBitMap = FixedBitMap(buffer.array())
        buffer = channel.readBuffer(FixedBitMap.getBytesSize(numberOfInodes), "BlockGroup.inodesBitMap")
        val inodesBitMap = FixedBitMap(buffer.array())
        val inodes = Array<INode>(numberOfInodes) { deserializeFromChannel(channel) }
        
        return BlockGroup(blockSize, numberOfBlocks, numberOfInodes, freeBlocksCount, freeInodesCount, dataBlocksOffset, blockBitMap, inodesBitMap, inodes)
    }
}

object SuperGroupSerializer: OutputChannelSerializer<SuperGroup> {
    private val SuperGroupSize = 8 * Int.SIZE_BYTES

    override fun serialize(channel: WritableByteChannel, value: SuperGroup) {
        var buffer = ByteBuffer.allocate(SuperGroupSize)
        buffer.putInt(value.blockSize)
        .putInt(value.totalBlocks)
        .putInt(value.totalInodes)
        .putInt(value.totalBlockGroups)
        .putInt(value.freeBlockCount)
        .putInt(value.freeInodeCount)
        .putInt(value.blockPerGroup)
        .putInt(value.inodesPerGroup)
        channel.write(buffer.flip())
    }

    override fun deserialize(channel: ReadableByteChannel): SuperGroup {
        val buffer = channel.readBuffer(SuperGroupSize, "SuperGroup")
        val blockSize = buffer.getInt()
        val totalBlocks = buffer.getInt()
        val totalInodes = buffer.getInt()
        val totalBlockGroups = buffer.getInt()
        val freeBlockCount = buffer.getInt()
        val freeInodeCount = buffer.getInt()
        val blockPerGroup = buffer.getInt()
        val inodesPerGroup = buffer.getInt()

        return SuperGroup(blockSize, totalBlocks, totalInodes, totalBlockGroups, freeBlockCount, freeInodeCount, blockPerGroup, inodesPerGroup)
    }
}

object INodeSerializer: OutputChannelSerializer<INode> {
    override fun serialize(channel: WritableByteChannel, value: INode) {
        val buffer = ByteBuffer.allocate(INode.sizeInBytes())
        buffer.putInt(value.id)
                .put(value.type.type)
        .putLong(value.dataSize)
        .putLong(value.created.toLong())
        .putLong(value.lastModified.toLong())
        value.blockOffsets.forEach { buffer.putLong(it) }
        channel.write(buffer.flip())
    }

    override fun deserialize(channel: ReadableByteChannel): INode {
        val buffer = channel.readBuffer(INode.sizeInBytes(), "INode")
        val id = buffer.getInt()
        val type = buffer.get()
        val dataSize = buffer.getLong()
        val created = buffer.getLong().toDate()
        val lm = buffer.getLong().toDate()
        val offsets = LongArray(INode.OFFSETS_SIZE)
        for(i in 0 until INode.OFFSETS_SIZE)
            offsets[i] = buffer.getLong()

        return INode(id, NodeType.fromByte(type), dataSize, created, lm, offsets)
    }
}

object FolderEntrySerializer: OutputChannelSerializer<FolderEntry> {
    private const val FolderEntryFixedPartSize = Int.SIZE_BYTES + Byte.SIZE_BYTES + Short.SIZE_BYTES
    override fun serialize(channel: WritableByteChannel, value: FolderEntry) {
        val name = value.name.toByteArray()
        val buffer = ByteBuffer.allocate(name.size + FolderEntryFixedPartSize)
        buffer.putInt(value.inodeId)
            .put(value.nodeType.type)
            .putShort(name.size.toShort())
            .put(name)
        channel.write(buffer.flip())
    }

    override fun deserialize(channel: ReadableByteChannel): FolderEntry {
        val buffer = channel.readBuffer(FolderEntryFixedPartSize, "FolderEntry")
        val inodeId = buffer.getInt()
        val nodeType = NodeType.fromByte(buffer.get())
        val stringSize = buffer.getShort()
        val nameBuffer = channel.readBuffer(stringSize.toInt(), "FolderEntry.name")

        return FolderEntry(inodeId, nodeType, nameBuffer.array().toString(Charsets.UTF_8))
    }
}

fun ReadableByteChannel.readBuffer(size: Int, name: String): ByteBuffer
{
    var buffer = ByteBuffer.allocate(size)
    if(this.read(buffer) != size)
        throw IOException("Cant read $name from disk")
    return buffer.flip()
}

fun WritableByteChannel.writeByte(value: Byte)
{
    var buffer = ByteBuffer.allocate(1)
    buffer.put(value)
    this.write(buffer.flip())
}

fun WritableByteChannel.writeInt(value: Int)
{
    var buffer = ByteBuffer.allocate(Int.SIZE_BYTES)
    buffer.putInt(value)
    this.write(buffer.flip())
}

fun WritableByteChannel.writeLong(value: Long)
{
    var buffer = ByteBuffer.allocate(Long.SIZE_BYTES)
    buffer.putLong(value)
    this.write(buffer.flip())
}


fun Date?.toLong():Long{
    if(this == null)
        return 0L
    return this.time
}

fun Long.toDate() : Date?{
    if(this == 0L)
        return null
    return Date(this)
}