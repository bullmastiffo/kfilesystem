package com.mvg.app

import arrow.core.Either
import arrow.core.Left
import com.mvg.virtualfs.*
import com.mvg.virtualfs.ViFileSystem
import com.mvg.virtualfs.initializeViFilesystem
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.StandardOpenOption

fun main(args: Array<String>) {
    val localFs = FileSystems.getDefault()

    println("Hello to virtual fs!\nmount <fileName>\tmount\nquit\nformat x x <fileName>\tFormat ~100Mb partition with 1kb block")
    var fs: ViFileSystem? = null
    while (true)
    {
        val cmd = readLine()?.split(' ') ?: break
        try {
            when (cmd[0]) {
                "quit","exit" -> break
                "mount" -> {
                    if (cmd.size < 2) {
                        println("too little parameters")
                        continue
                    }
                    val virtualFsPath = localFs.getPath(cmd[1])
                    if (!Files.exists(virtualFsPath)) {
                        println("File doesn't exist.")
                    }
                    val ch = Files.newByteChannel(virtualFsPath, StandardOpenOption.READ, StandardOpenOption.WRITE)
                    fs = when (val r = initializeViFilesystem(ch)) {
                        is Either.Left -> {
                            println("failed to initialize system ${r.a}")
                            continue
                        }
                        is Either.Right -> r.b
                    }
                    println("Mounted")
                }
                "format" -> {
                    if (cmd.size < 4) {
                        println("too little parameters")
                        continue
                    }
                    val sz = cmd[1].toLongOrNull() ?: 100L * (1L shl 20)
                    val block = cmd[2].toIntOrNull() ?: 1
                    val blockSz = when (block) {
                        1 -> BlockSize.Block1Kb
                        2 -> BlockSize.Block2Kb
                        else -> BlockSize.Block4Kb
                    }
                    val virtualFsPath = localFs.getPath(cmd[3])
                    if (Files.exists(virtualFsPath)) {
                        Files.delete(virtualFsPath)
                    }
                    val settings = ViFileSystemSettings(sz, blockSz)
                    formatViFileSystem(virtualFsPath, settings)
                    println("Formatted")
                }
                "resize" -> {
                    if (cmd.size < 3) {
                        println("too little parameters")
                        continue
                    }
                    val sz = cmd[2].toLongOrNull() ?: 100L * (1L shl 20)
                    val virtualFsPath = localFs.getPath(cmd[1])
                    if (!Files.exists(virtualFsPath)) {
                        println("file doesn't exist ${cmd[2]}")
                        continue
                    }
                    Files.newByteChannel(virtualFsPath, StandardOpenOption.READ, StandardOpenOption.WRITE).use {
                        when(val r = resizeViFilesystem(it, sz)){
                            is Either.Left -> println("Error resizing: ${r.a}")
                            is Either.Right -> println("Resized")
                        }
                    }
                }
                "ls" -> {
                    if (cmd.size < 2) {
                        println("too little parameters")
                        continue
                    }
                    if (fs == null) {
                        println("no file system")
                        continue
                    }
                    val path = cmd[1]
                    println("$path folder contains:")
                    fs.getFolderItems(path).forEach {
                        println("${it.name}\t\t${it.created}\t\t${it.lastModified}\t\t${it.type}\t{${it.size/1024}kb}")
                    }
                }
                "mkdir" -> {
                    if (fs == null) {
                        println("no file system")
                        continue
                    }
                    if (cmd.size < 3) {
                        println("too little parameters")
                        continue
                    }
                    val path = cmd[1]
                    val name = cmd[2]
                    val fi = fs.createFolder(path, name)
                    println("created ${fi.name}")
                }
                "rm" -> {
                    if (fs == null) {
                        println("no file system")
                        continue
                    }
                    if (cmd.size < 2) {
                        println("too little parameters")
                        continue
                    }
                    val path = cmd[1]
                    fs.deleteItem(path)
                    println("deleted $path")
                }
                "cp2vs" -> {
                    if (fs == null) {
                        println("no file system")
                        continue
                    }
                    if (cmd.size < 3) {
                        println("too little parameters")
                        continue
                    }
                    val pathVirtual = cmd[1]
                    val pathReal = localFs.getPath(cmd[2])
                    fs.createFile(pathVirtual, pathReal.fileName.toString()).use { dest ->
                        val buffer = ByteBuffer.allocate(fs.fileSystemInfo.blockSize)
                        Files.newByteChannel(pathReal, StandardOpenOption.READ, StandardOpenOption.WRITE).use { src ->
                            while(src.read(buffer) > 0)
                            {
                                dest.write(buffer.flip())
                                buffer.flip()
                            }
                        }
                    }
                    println("copied!")
                }
                "cp2real" -> {
                    if (fs == null) {
                        println("no file system")
                        continue
                    }
                    if (cmd.size < 3) {
                        println("too little parameters")
                        continue
                    }
                    val pathVirtual = cmd[1]
                    val pathReal = localFs.getPath(cmd[2])
                    fs.openFile(pathVirtual).use { src ->
                        val buffer = ByteBuffer.allocate(fs.fileSystemInfo.blockSize)
                        Files.newByteChannel(pathReal, StandardOpenOption.CREATE_NEW, StandardOpenOption.READ, StandardOpenOption.WRITE).use { dest ->
                            while(src.read(buffer) > 0)
                            {
                                dest.write(buffer.flip())
                                buffer.flip()
                            }
                        }
                    }
                    println("copied!")
                }
                else ->{
                    println("unknown")
                }
            }
        }
        catch (e: Throwable){
            println("Continue at your own risk! Exception: $e")
        }
    }
    fs?.close()
}
