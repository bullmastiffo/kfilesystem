package com.mvg.virtualfs

import com.mvg.virtualfs.core.ItemType
import java.util.*

data class FileInfo(
    val name: String,
    val type: ItemType,
    val created: Date,
    val lastModified: Date)
