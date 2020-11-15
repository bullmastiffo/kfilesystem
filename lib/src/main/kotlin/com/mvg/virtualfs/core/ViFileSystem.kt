package com.mvg.virtualfs.core

import com.mvg.virtualfs.FileInfo
import com.mvg.virtualfs.FileOpenMode
import com.mvg.virtualfs.FileSystem
import java.nio.channels.SeekableByteChannel

class ViFileSystem(private val rootFolder: FolderHandler) : FileSystem {

    override fun createFile(name: String): SeekableByteChannel {
        TODO("Not yet implemented")
    }

    override fun openFile(name: String, mode: FileOpenMode): SeekableByteChannel {
        TODO("Not yet implemented")
    }

    override fun deleteFile(name: String) {
        TODO("Not yet implemented")
    }

    override fun getFiles(path: String): Iterable<FileInfo> {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }

    companion object {
        const val PATH_DELIMITER = '/'
    }
}