package com.mvg.virtualfs;

import java.io.InputStream
import java.io.OutputStream
import java.nio.channels.SeekableByteChannel

enum class FileOpenMode {
    Read, Write
}

interface FileSystem {
    fun createFile(name: String): OutputStream
    fun openFile(name: String, mode: FileOpenMode): SeekableByteChannel
    fun readFile(name: String): InputStream
    fun deleteFile(name: String)
    fun getFiles(path: String?): Iterable<FileInfo>
}

