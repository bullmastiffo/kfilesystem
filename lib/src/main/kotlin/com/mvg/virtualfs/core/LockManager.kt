package com.mvg.virtualfs.core

import java.util.concurrent.locks.Lock

interface LockManager<T>{
    fun createFreeLock(): Lock
    fun registerLockForItem(item: T, lock: Lock)
    fun getLockForItem(item: T): Lock
}