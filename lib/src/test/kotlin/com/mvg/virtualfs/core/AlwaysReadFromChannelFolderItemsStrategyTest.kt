package com.mvg.virtualfs.core

import arrow.core.Either
import com.mvg.virtualfs.BlockSize
import com.mvg.virtualfs.storage.FolderEntry
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.locks.ReentrantLock

internal class AlwaysReadFromChannelFolderItemsStrategyTest : FolderItemsStrategyTestBase<AlwaysReadFromChannelFolderItemsStrategy>() {

    @Test
    fun containsItem() {
        for (e in folderEntries){
            val result = sut.containsItem(e.name, coreFileSystem, inodeAccessor)
            assert(result is Either.Right && result.b)
        }

        val result = sut.containsItem("NonExistentItemName", coreFileSystem, inodeAccessor)

        assert(result is Either.Right && !result.b)
    }

    @Test
    fun `Test listItems read one at a time and releases lock after last read`() {
        val expectedList = folderEntries.map {
            NamedItemDescriptor(it.name, descriptorMap[it.inodeId]!!)
        }.toList()
        val closeSlot = slot<() -> Unit>()
        every { inodeAccessor.getSeekableByteChannel(coreFileSystem, capture(closeSlot)) }.returns(channel)
        every { channel.close() }.answers { closeSlot.captured() }
        val l = ReentrantLock()
        l.lock()

        val result = sut.listItems(coreFileSystem, inodeAccessor, l)

        val iterable = (result as Either.Right).b
        var counter = 0
        iterable.zip(expectedList) { f, e ->
            counter++
            assertNamedItemDescriptorAreEqual(e, f)
            verify(exactly = counter){ deserialize(channel) }
            verify(exactly = counter) { coreFileSystem.getInodeItemDescriptor(any()) }
            assertTrue(l.isLocked)
        }
        assertFalse(l.isLocked)
    }

    @Test
    fun `Test getItem reads stream until finds item`() {
        repeat(folderEntries.size) {
            val folderEntry = folderEntries[it]
            val expected = NamedItemDescriptor(folderEntry.name, descriptorMap[folderEntry.inodeId]!!)

            val result = (sut.getItem(folderEntry.name, coreFileSystem, inodeAccessor) as Either.Right).b
            resetChannelState()
            assertNamedItemDescriptorAreEqual(expected, result)
        }
    }

    @Test
    fun `Test getItem returns error if not found`() {
        val result = (sut.getItem("nonExistentItemName", coreFileSystem, inodeAccessor) as Either.Left).a
        assertTrue(result is CoreFileSystemError.ItemNotFoundError)
    }

    @Test
    fun `Test addItem serializes entry and returns reread items`() {
        val newItem = NamedItemDescriptor(7, ItemType.Folder, mockk(), "newFolder")
        every { inodeAccessor.size }.returns(positionAfterTerminating)
        every { channel.position(positionAfterTerminating - FolderEntry.TerminatingEntrySize) }
                .returns(channel)
        every { inodeAccessor.getSeekableByteChannel(coreFileSystem, any()) }.returns(channel)
        val folderEntrySlot = slot<FolderEntry>()
        var call = 0
        every { serialize(channel, capture(folderEntrySlot)) }.answers {
            when(call++){
                0 -> {
                    assertEquals(newItem.nodeId, folderEntrySlot.captured.inodeId)
                    assertEquals(newItem.name, folderEntrySlot.captured.name)
                    assertEquals(newItem.type.toNodeType, folderEntrySlot.captured.nodeType)
                    folderEntries.add(folderEntrySlot.captured)
                    descriptorMap[newItem.nodeId] = newItem
                }
                1 -> {
                    assertEquals(FolderEntry.TerminatingEntry, folderEntrySlot.captured)
                }
                else -> fail("Not expected to write more")
            }
        }

        val added = (sut.addItem(newItem, coreFileSystem, inodeAccessor) as Either.Right).b

        val expectedListAfterAdd = folderEntries.map {
            NamedItemDescriptor(it.name, descriptorMap[it.inodeId]!!)
        }.toList()
        assertEquals(newItem, added)
        val factList = (sut.listItems(coreFileSystem, inodeAccessor, mockk(relaxed = true)) as Either.Right).b
        expectedListAfterAdd.zip(factList) { e, f -> assertNamedItemDescriptorAreEqual(e, f) }
    }

    @Test
    fun `Test deleteItem removes entry, copies remaining stream`() {
        val entryToDelete = folderEntries[3]
        every { channel.position(any()) }.returns(channel)
        every { inodeAccessor.getSeekableByteChannel(coreFileSystem, any()) }.returns(channel)
        every { channel.truncate(any()) }.returns(channel)
        var byteCounter = 1
        every { channel.read(any()) }.answers {  byteCounter-- }
        every { channel.write(any()) }.returns(byteCounter)
        every { coreFileSystem.fileSystemInfo.blockSize }.returns(BlockSize.Block1Kb.size)

        sut.deleteItem(entryToDelete.name, coreFileSystem, inodeAccessor)
    }

    override fun buildSut(): AlwaysReadFromChannelFolderItemsStrategy = AlwaysReadFromChannelFolderItemsStrategy(serialize, deserialize)
}