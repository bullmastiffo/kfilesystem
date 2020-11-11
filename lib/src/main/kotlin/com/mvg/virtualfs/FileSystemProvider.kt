package com.mvg.virtualfs

interface FileSystemProvider{
    fun create(fileName: String): FileSystem
    fun open(fileName: String): FileSystem
}