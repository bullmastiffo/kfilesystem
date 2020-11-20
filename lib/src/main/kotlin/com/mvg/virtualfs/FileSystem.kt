package com.mvg.virtualfs;

import java.io.Closeable
import java.nio.channels.SeekableByteChannel

/**
 * Provides FileSystem interface
 */
interface FileSystem : Closeable {
    /**
     * Creates a file in a given folder and returns @SeekableByteChannel to manipulate contents.
     * @param path String Containing folder
     * @param name String Filename
     * @return SeekableByteChannel
     */
    fun createFile(path: String, name: String): SeekableByteChannel

    /**
     * Opens existing file and returns @SeekableByteChannel to manipulate contents.
     * @param filePath String full path to file
     * @return SeekableByteChannel
     */
    fun openFile(filePath: String): SeekableByteChannel

    /**
     * Deletes given item (File or Folder). Folder must be empty to delete.
     * @param itemPath String
     */
    fun deleteItem(itemPath: String)

    /**
     * Gets list of items containing in a folder.
     * @param path String path a folder to list items in
     * @return Iterable<FileInfo>
     */
    fun getFolderItems(path: String): Iterable<FileInfo>

    /**
     * Creates subfolder in a given folder
     * @param path String Full path to a folder where to create a subfolder
     * @param name String Subfolder name
     * @return FileInfo
     */
    fun createFolder(path: String, name: String) : FileInfo
}

