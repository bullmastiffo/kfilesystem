package com.mvg.virtualfs.core

import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.management.InvalidApplicationException

interface ConcurrentPool<K, V>
    where V: Closeable {
    fun <T : V> getOrPut(key: K, factory: () -> T): T
    fun removeItem(key: K)
}

interface ProxyFactory<V> {
    fun <T : V> create(target: T, closeOverride: () -> Unit): T
}
/**
 * Creates object pool that is safe in multithreaded environment.
 * All callers will always get the same value for the same key
 * @param K Type of the key
 * @param V Type of the value, must be @see Closeable
 * @property proxyFactory Function2<[@kotlin.ParameterName] V, [@kotlin.ParameterName] Function0<Unit>, V>
 *     Proxy object factory for type V, that must create proxy for given target, replacing close method with provided.
 * @property map ConcurrentHashMap<K, Entry<K, V>>
 * @constructor
 */
class ReferenceCountingConcurrentPool<K, V>(
        private val proxyFactory: ProxyFactory<V>) : ConcurrentPool<K, V>
        where V: Closeable {

    private val map: ConcurrentHashMap<K, Entry<K, V>> = ConcurrentHashMap<K, Entry<K, V>>()

    /**
     * Gets value for given key if already in pool or invokes factory method to put it the pool.
     * Warning! Factory may be invoked, but different instance can be returned
     * if another thread was first to put it for this key. Created instance will be closed in this case.
     * @param key K
     * @param factory Function0<V>
     * @return V
     */
    override fun <T : V> getOrPut(key: K, factory: () -> T): T {
        var entry: Entry<K, V>?
        do {
            entry = map[key]
            val acquired = if (entry != null){
                entry.tryIncrement()
            } else {
                entry = Entry(key, factory())
                val otherEntry = map.putIfAbsent(key, entry)
                if (otherEntry != null) {
                    entry.value.close()
                    entry = otherEntry
                    entry.tryIncrement()
                } else {
                    true
                }
            }
        } while(!acquired)

        return proxyFactory.create(entry!!.value as T) { entry.close() }
    }

    override fun removeItem(key: K) {
        val entry = map.remove(key) ?: throw InvalidApplicationException("Item must be in the pool to remove")
        entry.value.close()
    }

    inner class Entry<K, V>(private val key: K, val value: V)
            where V: Closeable {
        private val referenceCounter: AtomicInteger = AtomicInteger(1)

        fun tryIncrement(): Boolean{
            do
            {
                val initialCounter = this.referenceCounter.get()
                val increasedValue = initialCounter + 1
                if (initialCounter <= 0)
                {
                    // in case we got zero it is disposed and we can't no longer use it.
                    // That's why compareExchange and not increment.
                    return false
                }
            }
            while (initialCounter != this.referenceCounter.compareAndExchange(initialCounter, increasedValue));

            return true
        }

        fun close(){
            val c = referenceCounter.decrementAndGet()
            if (c > 0){
                return
            }
            value.close()
            this@ReferenceCountingConcurrentPool.map.remove(key, this@Entry)
        }
    }
}