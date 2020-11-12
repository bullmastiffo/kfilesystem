package com.mvg.virtualfs.storage

import kotlinx.serialization.Serializable

@Serializable
data class FolderEntry(
        val inodeId: Int,
        val nodeType: NodeType,
        val name: String) {
}