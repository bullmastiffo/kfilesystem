package com.mvg.virtualfs.core

import arrow.core.right
import com.mvg.virtualfs.storage.FolderEntry
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.fail
import java.nio.channels.ReadableByteChannel
import java.nio.channels.SeekableByteChannel
import java.nio.channels.WritableByteChannel

abstract class FolderItemsStrategyTestBase<T : FolderItemsStrategy> {
    protected val folderEntries = arrayListOf(
            FolderEntry(1, NodeType.File, "file1"),
            FolderEntry(2, NodeType.Folder, "folder1"),
            FolderEntry(3, NodeType.File, "file2"),
            FolderEntry(4, NodeType.Folder, "folder2"))
    protected val descriptorMap = mutableMapOf(
            1 to ItemDescriptor(1, ItemType.File, mockk()),
            2 to ItemDescriptor(2, ItemType.Folder, mockk()),
            3 to ItemDescriptor(3, ItemType.File, mockk()),
            4 to ItemDescriptor(4, ItemType.Folder, mockk()))
    protected val positionAfterTerminating = 200L
    private var channelState = 0

    protected fun resetChannelState() {
        channelState = 0
    }

    @MockK
    protected lateinit var serialize: (channel: WritableByteChannel, value: FolderEntry) -> Unit

    @MockK
    protected lateinit var deserialize: (channel: ReadableByteChannel) -> FolderEntry
    protected lateinit var sut: T

    protected abstract fun buildSut(): T

    @MockK
    protected lateinit var coreFileSystem: CoreFileSystem

    @MockK
    protected lateinit var channel: SeekableByteChannel

    @MockK
    protected lateinit var inodeAccessor: INodeAccessor

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        sut = buildSut()
        every { coreFileSystem.getInodeItemDescriptor(any()) }.answers {
            val inodeId = firstArg<Int>()
            descriptorMap[inodeId]!!.right()
        }
        every { channel.position() }.returns(positionAfterTerminating)
        every { channel.close() }.returns(Unit)
        every { deserialize(channel) }.answers {
            val entry = when {
                channelState < folderEntries.size -> {
                    folderEntries[channelState++]
                }
                channelState == folderEntries.size -> {
                    channelState++
                    FolderEntry.TerminatingEntry
                }
                else -> {
                    fail("Trying to read channel")
                }
            }
            entry
        }
        every { inodeAccessor.getSeekableByteChannel(coreFileSystem) }.returns(channel)
    }

    protected fun assertNamedItemDescriptorAreEqual(expected: NamedItemDescriptor, fact: NamedItemDescriptor)
    {
        Assertions.assertEquals(expected.name, fact.name)
        Assertions.assertEquals(expected.nodeId, fact.nodeId)
        Assertions.assertEquals(expected.type, fact.type)
        Assertions.assertEquals(expected.attributeSet, fact.attributeSet)
    }
}