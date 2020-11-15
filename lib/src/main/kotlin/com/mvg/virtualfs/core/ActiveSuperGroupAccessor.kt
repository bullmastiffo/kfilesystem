package com.mvg.virtualfs.core

import com.mvg.virtualfs.storage.SuperGroup
import java.util.concurrent.atomic.AtomicInteger

class ActiveSuperGroupAccessor(private val superGroup: SuperGroup) : SuperGroupAccessor
{
    private val activeFreeBlockCount: AtomicInteger = AtomicInteger(superGroup.freeBlockCount)
    private val activeFreeInodesCount: AtomicInteger = AtomicInteger(superGroup.freeInodeCount)

    override val blockSize: Int
        get() = superGroup.blockSize
    override val totalBlocks: Int
        get() = superGroup.totalBlocks
    override val totalInodes: Int
        get() = superGroup.totalInodes
    override val totalBlockGroups: Int
        get() = superGroup.totalBlockGroups
    override val freeBlockCount: Int
        get() = activeFreeBlockCount.get()
    override val freeInodeCount: Int
        get() = activeFreeInodesCount.get()
    override val blockPerGroup: Int
        get() = superGroup.blockPerGroup
    override val inodesPerGroup: Int
        get() = superGroup.inodesPerGroup

    override fun incrementFreeBlockCount() {
        activeFreeBlockCount.incrementAndGet()
    }

    override fun decrementFreeBlockCount() {
        activeFreeBlockCount.decrementAndGet()
    }

    override fun incrementFreeInodeCount() {
        activeFreeInodesCount.incrementAndGet()
    }

    override fun decrementFreeInodeCount() {
        activeFreeInodesCount.decrementAndGet()
    }

}