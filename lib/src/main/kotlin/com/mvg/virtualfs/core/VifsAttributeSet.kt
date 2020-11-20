package com.mvg.virtualfs.core

import com.mvg.virtualfs.storage.INode
import java.util.*

class ViFsAttributeSet(private val inode: INode): AttributeSet {

    override val size: Long
        get() = inode.dataSize

    override val created: Date
        get() = inode.created!!
    override val lastModified: Date
        get() = inode.lastModified!!
}