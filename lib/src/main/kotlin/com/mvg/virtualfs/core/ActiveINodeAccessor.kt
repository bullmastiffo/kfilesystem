package com.mvg.virtualfs.core

import arrow.core.Either
import arrow.core.right
import com.mvg.virtualfs.storage.INode
import com.mvg.virtualfs.storage.INode.Companion.OFFSETS_SIZE
import com.mvg.virtualfs.storage.serialization.DuplexChannel
import com.mvg.virtualfs.storage.serialization.InputChannel
import com.mvg.virtualfs.storage.serialization.serializeToChannel
import java.util.concurrent.locks.Lock

class ActiveINodeAccessor(
        private val inode: INode,
        private val lock: Lock,
        private val viFsAttributeSet: ViFsAttributeSet) : INodeAccessor {
    override val id: Int
        get() = inode.id
    override var type: NodeType
        get() = inode.type
        set(value) { inode.type = value }
    override val attributeSet: AttributeSet
        get() = viFsAttributeSet

    override fun addDataBlock(coreFileSystem: CoreFileSystem, offset: Long): Either<CoreFileSystemError, Long> {
        val directNodesLength = OFFSETS_SIZE - 2
        var foundIndex = 1
        for (i in 0 until directNodesLength){
            if(inode.blockOffsets[i] == 0L)
            {
                foundIndex = i
                break
            }
        }
        if(foundIndex >= 0){
            inode.blockOffsets[foundIndex]=offset
            return offset.right()
        }
        TODO("Add indirect and double indirect blocks")
    }

    override fun serialize(channel: DuplexChannel) {
        inode.created = viFsAttributeSet.created
        inode.lastModified = viFsAttributeSet.lastModified
        serializeToChannel(channel, inode)
    }

    override fun getDataInputChannel(coreFileSystem: CoreFileSystem): InputChannel {
        TODO("Not yet implemented")
    }

    override fun close() {
        lock.unlock()
    }
}