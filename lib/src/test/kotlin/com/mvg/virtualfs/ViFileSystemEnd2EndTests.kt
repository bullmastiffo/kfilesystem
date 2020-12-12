package com.mvg.virtualfs

import arrow.core.Either
import com.mvg.virtualfs.core.ActiveINodeAccessor
import com.mvg.virtualfs.core.AlwaysReadFromChannelFolderItemsStrategy
import com.mvg.virtualfs.core.InMemoryFolderItemsStrategy
import com.mvg.virtualfs.storage.ceilDivide
import kotlinx.coroutines.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

internal class TestWithAlwaysReadFromChannelFolderItemsStrategy : ViFileSystemEnd2EndTests() {
    private val alwaysReadInstance = AlwaysReadFromChannelFolderItemsStrategy()
    override fun getViFileSystem(ch: SeekableByteChannel) =
            initializeViFilesystem(ch) { alwaysReadInstance }.fold(
                    { throw IOException("Filesystem not created: $it") },
                    { it })
}

internal class TestWithInMemoryFolderItemsStrategy : ViFileSystemEnd2EndTests() {
    override fun getViFileSystem(ch: SeekableByteChannel) =
            initializeViFilesystem(ch) { InMemoryFolderItemsStrategy() }.fold(
                    { throw IOException("Filesystem not created: $it") },
                    { it })
}

internal abstract class ViFileSystemEnd2EndTests {
    @Test
    fun `Test can create file with maximum size and then free space`(){
        val blockSize = BlockSize.Block1Kb.size
        val testFileSystemPath = createFileSystemInTempFile(BlockSize.Block1Kb)
        val testFileName = "someFile.bin"
        Files.newByteChannel(testFileSystemPath, StandardOpenOption.READ, StandardOpenOption.WRITE).use { ch ->
            val fs = getViFileSystem(ch)
            val info = fs.fileSystemInfo
            val maxFileSize = blockSize * (ActiveINodeAccessor.INDIRECT_INDEX + (blockSize / 8)*(1 + blockSize / 8))
            val blocksToWriteSize = blockSize * 2
            val toWrite = ceilDivide(maxFileSize, blocksToWriteSize)
            val remainder = maxFileSize % blocksToWriteSize
            fs.createFile("/", testFileName).use {
                val byteToWrite: Byte = 2
                val arrayToWrite = ByteArray(blocksToWriteSize) { byteToWrite }
                val buffer = ByteBuffer.wrap(arrayToWrite)
                var written = 0
                for(i in 0 until toWrite){
                    if (remainder > 0 && i == toWrite - 1){
                        buffer.limit(remainder)
                    }
                    written+= it.write(buffer)
                    buffer.flip()
                }
                assertEquals(maxFileSize, written)
            }
            assert(info.freeSize - maxFileSize >= fs.fileSystemInfo.freeSize)
            val items = fs.getFolderItems("/").toList()
            assertEquals(1, items.size)
            assertEquals(maxFileSize, items[0].size.toInt())
            fs.deleteItem("/$testFileName")
            assertEquals(info, fs.fileSystemInfo)
        }
    }

    @Test
    fun `Test can write and read files in parallel`() = runBlocking {
        val blockSize = BlockSize.Block2Kb.size
        val testFileSystemPath = createFileSystemInTempFile(BlockSize.Block2Kb)
        val testFileName = "someFile%s.bin"
        Files.newByteChannel(testFileSystemPath, StandardOpenOption.READ, StandardOpenOption.WRITE).use { ch ->
            val fs = getViFileSystem(ch)
            val coroutinesCount = 10
            val fileSize = blockSize * (ActiveINodeAccessor.INDIRECT_INDEX + (blockSize / 8))
            val writtenSizes = IntArray(coroutinesCount)
            withContext(Dispatchers.Default) {
                massiveRun(coroutinesCount) {i ->
                    delay(10)
                    fs.createFile( "/", String.format(testFileName, i)).use {
                        val byteToWrite: Byte = i.toByte()
                        val arrayToWrite = ByteArray(blockSize * 2) { byteToWrite }
                        val buffer = ByteBuffer.wrap(arrayToWrite)
                        var written = 0
                        while(written < fileSize){
                            written+= it.write(buffer)
                            buffer.flip()
                        }
                        writtenSizes[i] = written
                    }
                }
            }

            withContext(Dispatchers.Default) {
                massiveRun(coroutinesCount) { i ->
                    delay(10)
                    fs.openFile( "/" + String.format(testFileName, i)).use {
                        val byteToRead: Byte = i.toByte()
                        val buffer = ByteBuffer.allocate(blockSize / 2)
                        var readTotal = 0
                        do{
                            val read = it.read(buffer)
                            readTotal += read
                            for (b in 0 until read)
                            {
                                assertEquals(byteToRead, buffer.array()[b])
                            }
                            buffer.flip()
                        }while(read > 0)
                        assertEquals(writtenSizes[i], readTotal)
                    }
                }
            }
        }
    }

    @Test
    fun `Test can work with folders in parallel`() = runBlocking {
        val testFileSystemPath = createFileSystemInTempFile(BlockSize.Block2Kb)
        val testFileName = "someFile%s.bin"
        Files.newByteChannel(testFileSystemPath, StandardOpenOption.READ, StandardOpenOption.WRITE).use { ch ->
            val fs = getViFileSystem(ch)
            fs.createFolder("/", "f0")
            fs.createFolder("/f0", "f1")
            fs.createFolder("/f0/f1", "f2")
            fs.createFolder("/f0/f1/f2", "f3")
            val folderMap = hashMapOf(0 to "/f0", 1 to "/f0/f1", 2 to "/f0/f1/f2", 3 to "/f0/f1/f2/f3")

            val coroutinesCount = 40

            withContext(Dispatchers.Default) {
                massiveRun(coroutinesCount) {i ->
                    delay(10)
                    fs.createFile( folderMap[i % 4]!!, String.format(testFileName, i)).use {
                    }
                    delay(10)
                    fs.deleteItem(folderMap[i % 4]!! + "/" + String.format(testFileName, i))
                }
            }
        }
    }

    @Test
    fun `Test can create and remove folders`(){
        val testFileSystemPath = createFileSystemInTempFile(BlockSize.Block1Kb)
        Files.newByteChannel(testFileSystemPath, StandardOpenOption.READ, StandardOpenOption.WRITE).use { ch ->
            val fs = getViFileSystem(ch)

            val info = fs.fileSystemInfo
            fs.createFolder("/", "f0")
            val numberOfFolders = 10
            for (i in 0 until numberOfFolders){
                fs.createFolder("/f0", "sub$i")
            }
            assertEquals(info.freeInodes - numberOfFolders - 1, fs.fileSystemInfo.freeInodes)
            assertThrows<IOException> { fs.deleteItem("/f0") }
            for (i in 0 until numberOfFolders){
                fs.deleteItem("/f0/sub$i")
            }
            fs.deleteItem("/f0")
            assertEquals(info, fs.fileSystemInfo)
        }
    }

    protected abstract fun getViFileSystem(ch: SeekableByteChannel): ViFileSystem

    private fun createFileSystemInTempFile(blockSize: BlockSize): Path {
        val file = createTempFile()
        file.deleteOnExit()
        val localFs = FileSystems.getDefault()
        val blockGroupSize = 8 * blockSize.size * blockSize.size
        val testSettings = ViFileSystemSettings(blockGroupSize * 3L, blockSize)

        val testFileSystemPath = localFs.getPath(file.absolutePath)
        Files.newByteChannel(testFileSystemPath, StandardOpenOption.READ, StandardOpenOption.WRITE).use {
            formatViFileSystem(it, testSettings)
        }
        return testFileSystemPath
    }
}