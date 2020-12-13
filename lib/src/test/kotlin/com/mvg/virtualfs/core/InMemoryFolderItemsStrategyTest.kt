package com.mvg.virtualfs.core

import arrow.core.Either
import com.mvg.virtualfs.massiveRun
import com.mvg.virtualfs.storage.FolderEntry
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.locks.ReentrantLock

internal class InMemoryFolderItemsStrategyTest : FolderItemsStrategyTestBase<InMemoryFolderItemsStrategy>() {

    @Test
    fun `Test containsItem reads once and returns true for existing entries and false otherwise`() {

        for (e in folderEntries){
            val result = sut.containsItem(e.name, coreFileSystem, inodeAccessor)
            assert(result is Either.Right && result.b)
        }
        val result = sut.containsItem("NonExistentItemName", coreFileSystem, inodeAccessor)
        assert(result is Either.Right && !result.b)
        verify(exactly = folderEntries.size + 1){ deserialize(channel) }
        verify(exactly = folderEntries.size) { coreFileSystem.getInodeItemDescriptor(any()) }
    }

    @Test
    fun `Test containsItem reads once and returns appropriate value concurrently`() = runBlocking {
        withContext(Dispatchers.Default) {
            massiveRun(10) {
                for (e in folderEntries) {
                    val result = sut.containsItem(e.name, coreFileSystem, inodeAccessor)
                    assert(result is Either.Right && result.b)
                }
                val result = sut.containsItem("NonExistentItemName", coreFileSystem, inodeAccessor)
                assert(result is Either.Right && !result.b)
            }
        }
        verify(exactly = folderEntries.size + 1) { deserialize(channel) }
        verify(exactly = folderEntries.size) { coreFileSystem.getInodeItemDescriptor(any()) }
    }

    @Test
    fun `Test listItems returns all items concurrently reading once`() = runBlocking {
        val expectedList = folderEntries.map {
            NamedItemDescriptor(it.name, descriptorMap[it.inodeId]!!)
        }.toList()
        withContext(Dispatchers.Default) {
            massiveRun(10) {
                val l = ReentrantLock()
                l.lock()

                val result = sut.listItems(coreFileSystem, inodeAccessor, l)

                val iterable = (result as Either.Right).b
                expectedList.zip(iterable) { e, f -> assertNamedItemDescriptorAreEqual(e, f) }
                assertFalse(l.isLocked)
            }
        }
        verify(exactly = folderEntries.size + 1){ deserialize(channel) }
        verify(exactly = folderEntries.size) { coreFileSystem.getInodeItemDescriptor(any()) }
    }

    @Test
    fun `Test getItem runs concurrently and returns item`() = runBlocking {
        withContext(Dispatchers.Default) {
            massiveRun(folderEntries.size * 4) {
                val folderEntry = folderEntries[it % 4]
                val expected = NamedItemDescriptor(folderEntry.name, descriptorMap[folderEntry.inodeId]!!)

                val result = (sut.getItem(folderEntry.name, coreFileSystem, inodeAccessor) as Either.Right).b

                assertNamedItemDescriptorAreEqual(expected, result)
            }
        }
        verify(exactly = folderEntries.size + 1){ deserialize(channel) }
        verify(exactly = folderEntries.size) { coreFileSystem.getInodeItemDescriptor(any()) }
    }

    @Test
    fun `Test addItem serializes entry and returns items without reread`() {
        val newItem = NamedItemDescriptor(7, ItemType.Folder, mockk(), "newFolder")
        every { channel.position(positionAfterTerminating - FolderEntry.TerminatingEntrySize) }
                .returns(channel)
        val folderEntrySlot = slot<FolderEntry>()
        var call = 0
        every { serialize(channel, capture(folderEntrySlot)) }.answers {
            when(call++){
                0 -> {
                    assertEquals(newItem.nodeId, folderEntrySlot.captured.inodeId)
                    assertEquals(newItem.name, folderEntrySlot.captured.name)
                    assertEquals(newItem.type.toNodeType, folderEntrySlot.captured.nodeType)
                }
                1 -> {
                    assertEquals(FolderEntry.TerminatingEntry, folderEntrySlot.captured)
                }
                else -> fail("Not expected to write more")
            }
        }
        val expectedListAfterAdd = folderEntries.map {
            NamedItemDescriptor(it.name, descriptorMap[it.inodeId]!!)
        }.toMutableList()
        expectedListAfterAdd.add(newItem)

        val added = (sut.addItem(newItem, coreFileSystem, inodeAccessor) as Either.Right).b

        assertEquals(newItem, added)
        val factList = (sut.listItems(coreFileSystem, inodeAccessor, mockk(relaxed = true)) as Either.Right).b
        expectedListAfterAdd.zip(factList) { e, f -> assertNamedItemDescriptorAreEqual(e, f) }
    }

    @Test
    fun `Test deleteItem removes entry, serializes all items and returns without reread`() {
        val entryToDelete = folderEntries[0]
        val expectedListAfterDelete = folderEntries.drop(1).map {
            NamedItemDescriptor(it.name, descriptorMap[it.inodeId]!!)
        }.toList()
        every { channel.position(0) }.returns(channel)
        every { channel.truncate(positionAfterTerminating) }.returns(channel)
        val folderEntrySlot = slot<FolderEntry>()
        var call = 0
        every { serialize(channel, capture(folderEntrySlot)) }.answers {
            when(call++){
                in expectedListAfterDelete.indices -> {
                    val item = expectedListAfterDelete[call - 1]
                    assertEquals(item.nodeId, folderEntrySlot.captured.inodeId)
                    assertEquals(item.name, folderEntrySlot.captured.name)
                    assertEquals(item.type.toNodeType, folderEntrySlot.captured.nodeType)
                }
                expectedListAfterDelete.size -> {
                    assertEquals(FolderEntry.TerminatingEntry, folderEntrySlot.captured)
                }
                else -> fail("Not expected to write more")
            }
        }

        sut.deleteItem(entryToDelete.name, coreFileSystem, inodeAccessor)

        val factList = (sut.listItems(coreFileSystem, inodeAccessor, mockk(relaxed = true)) as Either.Right).b
        expectedListAfterDelete.zip(factList) { e, f -> assertNamedItemDescriptorAreEqual(e, f) }
    }

    override fun buildSut(): InMemoryFolderItemsStrategy = InMemoryFolderItemsStrategy(serialize, deserialize)
}