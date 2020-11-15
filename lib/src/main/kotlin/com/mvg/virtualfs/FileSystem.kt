package com.mvg.virtualfs;

import java.io.Closeable
import java.nio.channels.SeekableByteChannel

enum class FileOpenMode {
    Read, Write
}

interface FileSystem : Closeable {
    fun createFile(name: String): SeekableByteChannel
    fun openFile(name: String, mode: FileOpenMode): SeekableByteChannel
    fun deleteFile(name: String)
    fun getFiles(path: String): Iterable<FileInfo>
}

