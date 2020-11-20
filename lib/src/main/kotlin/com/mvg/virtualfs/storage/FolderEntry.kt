package com.mvg.virtualfs.storage

import com.mvg.virtualfs.core.NodeType
import com.mvg.virtualfs.storage.serialization.OutputChannelSerializable

/**
 * Represents folder entry record of ViFileSystem as written to underlying channel.
 * @property inodeId Int Inode identifier
 * @property nodeType NodeType type of Inode
 * @property name String Name of the item.
 * @constructor
 */
@OutputChannelSerializable(with = FolderEntrySerializer::class)
data class FolderEntry(
        val inodeId: Int,
        val nodeType: NodeType,
        val name: String) {

    companion object {
        val TerminatingEntry = FolderEntry(0, NodeType.None, "")
        val TerminatingEntrySize = 7
    }
}