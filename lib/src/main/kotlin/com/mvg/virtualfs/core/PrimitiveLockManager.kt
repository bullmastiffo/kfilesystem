package com.mvg.virtualfs.core

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

class PrimitiveLockManager<T> : LockManager<T> {
    private val map: ConcurrentHashMap<T, Lock> = ConcurrentHashMap<T, Lock>()

    override fun createFreeLock(): Lock {
        return ReentrantLock()
    }

    override fun registerLockForItem(item: T, lock: Lock) {
        map[item] = lock
    }

    override fun getLockForItem(item: T): Lock {
        return map.getOrPut(item, { createFreeLock()})
    }
}