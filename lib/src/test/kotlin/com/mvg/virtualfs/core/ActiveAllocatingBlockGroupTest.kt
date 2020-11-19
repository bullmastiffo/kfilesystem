package com.mvg.virtualfs.core

import arrow.core.Either
import com.mvg.virtualfs.storage.BlockGroup
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.locks.Lock

/**
 * Tests for @see ActiveAllocatingBlockGroup.
 * sut - system under test, thus instance of ActiveAllocatingBlockGroup
 */
internal class ActiveAllocatingBlockGroupTest {
    private val blockGroupId = 1
    private val firstNodeIndex = 15

    @Test
    fun getIdReturnsBlockGroupId() {
        val (sut, _) = buildActiveAllocatingBlockGroupForTest()

        assertEquals(blockGroupId, sut.id)
    }

    @Test
    fun getFreeBlocksShouldBeEqualBlockGroupCount() {
        val (sut, blockGroup) = buildActiveAllocatingBlockGroupForTest()

        assertEquals(blockGroup.freeBlocksCount, sut.freeBlocks)
    }

    @Test
    fun reserveBlockAndGetOffsetConcurrentlyReservesAllAvailableBlocks()  = runBlocking {
        val (sut, blockGroup) = buildActiveAllocatingBlockGroupForTest()
        val freeBlockCount =blockGroup.freeBlocksCount
        val coroutinesCount = freeBlockCount + 2
        val serializerMock = spyk<FileSystemSerializer>()
        val results = Array<Either<CoreFileSystemError, Long>?>(coroutinesCount) { null }

        withContext(Dispatchers.Default) {
            massiveRun(coroutinesCount) {
                results[it] = sut.reserveBlockAndGetOffset(serializerMock)
            }
        }

        assertEquals(freeBlockCount, results.count { it is Either.Right })
        assertEquals(
                coroutinesCount - freeBlockCount,
                results.count { it is Either.Left && it.a == CoreFileSystemError.VolumeIsFullError  })
        // assert that next clear bit is out of range and BitMap has larger capacity.
        assertEquals(freeBlockCount, blockGroup.blockBitMap.nextClearBit())
        assertEquals(0, sut.freeBlocks)
        assertEquals(0, blockGroup.freeBlocksCount)
    }

    @Test
    fun reserveBlockReturnsOffsetsWithBlockSizeStep() {
        val (sut, blockGroup) = buildActiveAllocatingBlockGroupForTest()
        val freeBlockCount =blockGroup.freeBlocksCount
        val serializerMock = spyk<FileSystemSerializer>()
        for (i in 0 until freeBlockCount)
        {
            val result = sut.reserveBlockAndGetOffset(serializerMock)

            assert(result is Either.Right)
            val offset = (result as Either.Right).b
            val expectedOffset = blockGroup.dataBlocksOffset + blockGroup.blockSize * i
            assertEquals(expectedOffset, offset )
        }
        val result = sut.reserveBlockAndGetOffset(serializerMock)
        assert(result is Either.Left)
    }

    @Test
    fun markBlockFreeFreesTheBlockInBitmapAndIncrementsCounter() {
        val (sut, blockGroup) = buildActiveAllocatingBlockGroupForTest()
        val freeBlockCount =blockGroup.freeBlocksCount
        val serializerMock = spyk<FileSystemSerializer>()
        val offsets = LongArray(freeBlockCount)
        for (i in 0 until freeBlockCount)
        {
            offsets[i] =  (sut.reserveBlockAndGetOffset(serializerMock) as Either.Right).b
        }

        for (i in 0 until freeBlockCount) {
            assertEquals(true, blockGroup.blockBitMap.getBit(i))
            sut.markBlockFree(serializerMock, offsets[i])
            assertEquals(false, blockGroup.blockBitMap.getBit(i))
        }
        assertEquals(freeBlockCount, sut.freeBlocks)
        assertEquals(freeBlockCount, blockGroup.freeBlocksCount)
    }

    @Test
    fun getFreeINodesShouldBeEqualBlockGroupCount() {
        val (sut, blockGroup) = buildActiveAllocatingBlockGroupForTest()
        assertEquals(blockGroup.freeInodesCount, sut.freeInodes)
    }

    @Test
    fun reserveInodeConcurrentlyReservesAllAvailableInodeAndReturnsAccessors()  = runBlocking {
        val (sut, blockGroup) = buildActiveAllocatingBlockGroupForTest()
        val freeInodeCount = blockGroup.freeInodesCount
        val coroutinesCount = freeInodeCount + 2
        val coreFileSystem = mockk<CoreFileSystem>(relaxed = true)
        val dt = Date(2020)
        every { coreFileSystem.time.now() } returns dt
        val lockMock = spyk<Lock>()
        val results = Array<Either<CoreFileSystemError, INodeAccessor>?>(coroutinesCount) { null }

        withContext(Dispatchers.Default) {
            massiveRun(coroutinesCount) {
                results[it] = sut.reserveInode(coreFileSystem, lockMock)
            }
        }

        assertEquals(freeInodeCount, results.count { it is Either.Right })
        assertEquals(
                coroutinesCount - freeInodeCount,
                results.count { it is Either.Left && it.a == CoreFileSystemError.CantCreateMoreItemsError })
        // assert that next clear bit is out of range and BitMap has larger capacity.
        assertEquals(freeInodeCount, blockGroup.inodesBitMap.nextClearBit())
        assertEquals(0, sut.freeInodes)
        assertEquals(0, blockGroup.freeInodesCount)
    }

    @Test
    fun reserveInodeReturnsCorrectInodeAccessors() {
        val (sut, blockGroup) = buildActiveAllocatingBlockGroupForTest()
        val freeInodeCount = blockGroup.freeInodesCount
        val coreFileSystem = mockk<CoreFileSystem>(relaxed = true)
        val dt = Date(2020)
        every { coreFileSystem.time.now() } returns dt
        val lockMock = spyk<Lock>()

       for(i in 0 until freeInodeCount) {
           val result =  sut.reserveInode(coreFileSystem, lockMock)
           assert(result is Either.Right)
           val inodeAccessor = (result as Either.Right).b
           assertEquals(i + firstNodeIndex, inodeAccessor.id)
           assertEquals(dt, inodeAccessor.attributeSet.created)
           assertEquals(dt, inodeAccessor.attributeSet.lastModified)
       }
        val result =  sut.reserveInode(coreFileSystem, lockMock)
        assert(result is Either.Left)
    }

    @Test
    fun markInodeFreeReleasesInode() {
        val (sut, blockGroup) = buildActiveAllocatingBlockGroupForTest()
        val freeInodeCount = blockGroup.freeInodesCount
        val coreFileSystem = mockk<CoreFileSystem>(relaxed = true)
        val dt = Date(2020)
        every { coreFileSystem.time.now() } returns dt
        val lockMock = spyk<Lock>()
        val inodes = Array(freeInodeCount){
            (sut.reserveInode(coreFileSystem, lockMock) as Either.Right).b
        }

        for (i in 0 until freeInodeCount) {
            assertEquals(true, blockGroup.inodesBitMap.getBit(inodes[i].id - firstNodeIndex))
            sut.markInodeFree(coreFileSystem, inodes[i])
            assertEquals(false, blockGroup.inodesBitMap.getBit(inodes[i].id - firstNodeIndex))
        }
        assertEquals(freeInodeCount, sut.freeInodes)
        assertEquals(freeInodeCount, blockGroup.freeInodesCount)
    }

    private suspend fun massiveRun(times: Int, action: suspend (Int) -> Unit) {
        coroutineScope {
            repeat(times) {
                launch {
                    action(it)
                }
            }
        }
    }

    private fun buildActiveAllocatingBlockGroupForTest(): Pair<ActiveAllocatingBlockGroup, BlockGroup>
    {
        val blockGroup = BlockGroup(1024, 10, firstNodeIndex, 2048, firstNodeIndex)
        val sut = ActiveAllocatingBlockGroup(blockGroupId, blockGroup)
        return Pair(sut, blockGroup)
    }
}