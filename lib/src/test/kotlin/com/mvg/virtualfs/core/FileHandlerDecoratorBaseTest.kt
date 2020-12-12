package com.mvg.virtualfs.core

import arrow.core.right
import com.mvg.virtualfs.testDecoratorCallProxiesToTarget
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.nio.channels.SeekableByteChannel

internal class TestFileHandlerDecorator(target: FileHandler): FileHandlerDecoratorBase(target)

internal class FileHandlerDecoratorBaseTestMethodsProxyToTarget {

    @Test
    fun getSize() {
        val expectedSize = 13L
        testFileHandlerTarget(expectedSize) { it.size }
    }

    @Test
    fun getByteChannel() {
        val expectedChannel = mockk<SeekableByteChannel>().right()
        testFileHandlerTarget(expectedChannel) { it.getByteChannel()  }
    }

    @Test
    fun getDescriptor() {
        val expectedDescriptor = mockk<NamedItemDescriptor>()
        testFileHandlerTarget(expectedDescriptor) { it.descriptor }
    }

    @Test
    fun close() {
        testFileHandlerTarget(Unit) { it.close() }
    }

    @Test
    fun delete() {
        testFileHandlerTarget(Unit.right()) { it.delete() }
    }

    private fun <T> testFileHandlerTarget(expected: T, testCall: (fh: FileHandler) -> T){
        testDecoratorCallProxiesToTarget(expected, { TestFileHandlerDecorator(it) }, testCall)
    }
}