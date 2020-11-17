package com.mvg.virtualfs.core

import arrow.core.Either
import com.mvg.virtualfs.FileInfo
import com.mvg.virtualfs.FileOpenMode
import com.mvg.virtualfs.FileSystem
import java.io.Closeable
import java.io.IOException
import java.nio.channels.SeekableByteChannel
import java.util.concurrent.atomic.AtomicBoolean

class ViFileSystem(
        private val rootFolder: FolderHandler,
        private val token: Closeable) : FileSystem {
    private val isClosed = AtomicBoolean(false)

    override fun createFile(path: String, name: String): SeekableByteChannel {
        TODO("Not yet implemented")
    }

    override fun openFile(name: String, mode: FileOpenMode): SeekableByteChannel {
        TODO("Not yet implemented")
    }

    override fun deleteFile(name: String) {
        TODO("Not yet implemented")
    }

    override fun getFiles(path: String): Iterable<FileInfo> {
        var folder = navigateToFolder(path)
        val list = unwrapOrThrow({ folder.listItems() }, "Error listing items %s")
        folder.close()

        return list.map { FileInfo(it.name, it.type, it.attributeSet.created, it.attributeSet.lastModified) }
    }

    override fun createFolder(path: String, name: String): FileInfo {
        TODO("Not yet implemented")
    }

    override fun deleteFolder(path: String) {
        TODO("Not yet implemented")
    }

    override fun close() {
        if(isClosed.getAndSet(true))
            return
        rootFolder.close()
        token.close()
    }

    private fun navigateToFolder(path: String): FolderHandler {
        if(isClosed.get())
            throw IOException("filesystem unmounted")

        if (path.length == 0 || path[0] != PATH_DELIMITER) {
            throw IOException("path must start with $PATH_DELIMITER")
        }
        var folder = rootFolder
        path.split(PATH_DELIMITER).forEach {
            if (it != "") {
                val item = unwrapOrThrow({ folder.getItem(it) }, "Error reading folder %s")
                if (item !is FolderHandler) {
                    item.close()
                    throw IOException("item $it is not a folder")
                }
                if (folder != rootFolder)
                    folder.close()
                folder = item
            }
        }
        return folder
    }

    private fun<T> unwrapOrThrow(action:()-> Either<CoreFileSystemError, T>, errorTemplate: String): T
    {
        return when(val r = action()){
            is Either.Left -> throw IOException(String.format(errorTemplate, r.a))
            is Either.Right -> r.b
        }
    }

    companion object {
        const val PATH_DELIMITER = '/'
    }
}