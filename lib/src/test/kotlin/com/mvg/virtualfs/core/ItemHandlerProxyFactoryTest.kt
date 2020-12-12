package com.mvg.virtualfs.core

import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import javax.management.InvalidApplicationException

internal class ItemHandlerProxyFactoryTest {

    @Test
    fun `Test create folder decorator`() {
        val sut = ItemHandlerProxyFactory()
        val target = mockk<FolderHandler>()
        val action = spyk<()->Unit>()

        val result = sut.create(target, action)
        result.close()

        assertTrue(result is DeferredCloseFolderHandlerDecorator)
        verify { action() }
    }

    @Test
    fun `Test create file decorator`() {
        val sut = ItemHandlerProxyFactory()
        val target = mockk<FileHandler>()
        val action = spyk<()->Unit>()

        val result = sut.create(target, action)
        result.close()

        assertTrue(result is DeferredCloseFileHandlerDecorator)
        verify { action() }
    }

    @Test
    fun `Test create throws for unsupported type`() {
        val sut = ItemHandlerProxyFactory()
        val target = mockk<ItemHandler>()
        val action = spyk<()->Unit>()

        assertThrows(InvalidApplicationException::class.java) {
            sut.create(target, action)
        }
    }
}