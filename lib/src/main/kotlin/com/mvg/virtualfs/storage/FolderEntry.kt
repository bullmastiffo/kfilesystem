package com.mvg.virtualfs.storage

data class FolderEntry(
        val inodeId: Int,
        val nodeType: NodeType,
        val name: String) {
}