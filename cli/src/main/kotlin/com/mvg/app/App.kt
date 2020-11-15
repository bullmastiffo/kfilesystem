package com.mvg.app

import com.mvg.virtualfs.*
import java.nio.file.FileSystems
import java.nio.file.Files

fun main(args: Array<String>) {
    val fileName = "D:\\temp\\vi.fs"
    val localFs = FileSystems.getDefault()
    val virtualFsPath = localFs.getPath(fileName)
    if(Files.exists(virtualFsPath))
    {
        Files.delete(virtualFsPath)
    }
    var settins = ViFileSystemSettings(100L * (1L shl 20), BlockSize.Block1Kb)
    formatViFileSystem(virtualFsPath, settins)
    println("Hello World!")
}
