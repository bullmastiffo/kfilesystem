package com.mvg.app

import arrow.core.Either
import com.mvg.virtualfs.*
import com.mvg.virtualfs.core.ViFileSystem
import com.mvg.virtualfs.core.initializeViFilesystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.StandardOpenOption

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
    val ch = Files.newByteChannel(virtualFsPath, StandardOpenOption.READ, StandardOpenOption.WRITE)
    val fs = when(val r = initializeViFilesystem(ch)){
        is Either.Left -> {
            println("failed to initialize system ${r.a}")
            return
        }
        is Either.Right -> r.b
    }.use {
        println("Root folder contains:")
        it.getFiles("${ViFileSystem.PATH_DELIMITER}").forEach {
            println("${it.name}\t\t${it.created}\t\t${it.lastModified}\t\t${it.type}")
        }
    }
}
