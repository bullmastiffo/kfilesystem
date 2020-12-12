package com.mvg.virtualfs.core

import com.mvg.virtualfs.massiveRun
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.io.Closeable
import javax.management.InvalidApplicationException

internal interface TestCloseable : Closeable {
    fun instance(): Closeable
}

internal class ReferenceCountingConcurrentPoolTest {

    @Test
    fun `Test getOrPut always return same instance of stored item while it is in use`() {
        val sut = getReferenceCountingConcurrentPool()
        val instance = mockk<TestCloseable>()
        val key = 2

        val result = sut.getOrPut(key) { instance }
        val result2 = sut.getOrPut(key) { mockk() }
        val result3 = sut.getOrPut(key) { mockk() }

        assertEquals(instance, result.instance())
        assertEquals(instance, result2.instance())
        assertEquals(instance, result3.instance())
        result.close()
        result2.close()
        val result4 = sut.getOrPut(key) { mockk() }
        assertEquals(instance, result4.instance())
    }

    @Test
    fun `Test instance is removed from pool when all requested items are closed`() {
        val sut = getReferenceCountingConcurrentPool()
        val instance = mockk<TestCloseable>()
        every { instance.close() }.returns(Unit)
        val key = 2

        val result = sut.getOrPut(key) { instance }
        val result2 = sut.getOrPut(key) { mockk() }
        result.close()
        result2.close()
        val result3 = sut.getOrPut(key) { mockk() }

        assertNotEquals(instance, result3.instance())
        verify(exactly = 1) { instance.close() }
    }

    @Test
    fun `Test getOrPut always return same instance concurrently and closes unused`() = runBlocking {
        val sut = getReferenceCountingConcurrentPool()
        val key = 3
        val coroutinesCount = 20
        val results = Array<TestCloseable?>(coroutinesCount) { null }
        val created = ArrayList<TestCloseable>()

        withContext(Dispatchers.Default) {
            massiveRun(coroutinesCount) { i ->
                delay(10)
                results[i] = sut.getOrPut(key) {
                    val t = mockk<TestCloseable>()
                    every { t.close() }.returns(Unit)
                    created.add(t)
                    t
                }
            }
        }

        for (i in 1 until coroutinesCount){
            assertEquals(results[i-1]!!.instance(), results[i]!!.instance())
        }
        val m = results[0]!!.instance()
        verify(exactly = 0){ m.close() }
        created.forEach {
            val tm = it
            if(tm == m)
                return@forEach
            verify(exactly = 1) { tm.close() }
        }
    }

    @Test
    fun `Test getOrPut always return non closed instance concurrently`() = runBlocking {
        val sut = getReferenceCountingConcurrentPool()
        val key = 3
        val coroutinesCount = 20
        val created = ArrayList<TestCloseable>()

        withContext(Dispatchers.Default) {
            massiveRun(coroutinesCount) {
                delay(2)
                val m = sut.getOrPut(key) {
                    val t = mockk<TestCloseable>()
                    every { t.close() }.returns(Unit)
                    created.add(t)
                    t
                }
                verify(exactly = 0){ m.close() }
                m.close()
            }
        }

        created.forEach { tm ->
            verify(exactly = 1) { tm.close() }
        }
    }


    @Test
    fun `Test removeItem throws if item is not in pool`() {
        val sut = getReferenceCountingConcurrentPool()
        val nonExistentItemKey = 2
        assertThrows(InvalidApplicationException::class.java){
            sut.removeItem(nonExistentItemKey)
        }
    }

    @Test
    fun `Test removeItem removes item and closes contained item`() {
        val sut = getReferenceCountingConcurrentPool()
        val instance = mockk<TestCloseable>()
        every { instance.close() }.returns(Unit)
        val key = 2
        sut.getOrPut(key) { instance }
        sut.removeItem(key)
        verify(exactly = 1) { instance.close() }
    }

    private fun getReferenceCountingConcurrentPool() : ReferenceCountingConcurrentPool<Int, TestCloseable> {
        val proxyFactory = mockk<ProxyFactory<TestCloseable>>()
        every { proxyFactory.create(any(), any()) }.answers {
            val proxy = mockk<TestCloseable>()
            val instance = firstArg<TestCloseable>()
            val closer = secondArg<() -> Unit>()
            every { proxy.close() }.answers { closer() }
            every { proxy.instance() }.returns(instance)
            proxy
        }
        return ReferenceCountingConcurrentPool(proxyFactory)
    }
}