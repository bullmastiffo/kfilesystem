package com.mvg.virtualfs.core

import arrow.core.right
import com.mvg.virtualfs.testDecoratorCallProxiesToTarget
import io.mockk.mockk
import org.junit.jupiter.api.Test

internal class TestFolderHandlerDecorator(target: FolderHandler): FolderHandlerDecoratorBase(target)

internal class FolderHandlerDecoratorBaseTest {

    @Test
    fun listItems() {
        testFolderHandlerTarget(List<ItemDescriptor>(0) { mockk() }.right()) { it.listItems() }
    }

    @Test
    fun getItem() {
        testFolderHandlerTarget(mockk<ItemHandler>().right()) { it.getItem("itemToGetName") }
    }

    @Test
    fun createItem() {
        val itemTemplate = mockk<ItemTemplate>()
        testFolderHandlerTarget(mockk<ItemHandler>().right()) { it.createItem(itemTemplate) }
    }

    @Test
    fun deleteItem() {
        testFolderHandlerTarget(Unit.right()) { it.deleteItem("itemToDeleteName") }
    }

    @Test
    fun getDescriptor() {
        val expectedDescriptor = mockk<NamedItemDescriptor>()
        testFolderHandlerTarget(expectedDescriptor) { it.descriptor }
    }

    @Test
    fun close() {
        testFolderHandlerTarget(Unit) { it.close() }
    }

    @Test
    fun delete() {
        testFolderHandlerTarget(Unit.right()) { it.delete() }
    }

    private fun <T> testFolderHandlerTarget(expected: T, testCall: (fh: FolderHandler) -> T){
        testDecoratorCallProxiesToTarget(expected, { TestFolderHandlerDecorator(it) }, testCall)
    }
}