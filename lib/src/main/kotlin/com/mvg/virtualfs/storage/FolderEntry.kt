package com.mvg.virtualfs.storage

import com.mvg.virtualfs.storage.serialization.OutputChannelSerializable


@OutputChannelSerializable(with = FolderEntrySerializer::class)
data class FolderEntry(
        val inodeId: Int,
        val nodeType: NodeType,
        val name: String) {
}