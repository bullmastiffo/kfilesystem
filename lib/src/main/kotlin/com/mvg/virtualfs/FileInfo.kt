package com.mvg.virtualfs

import com.mvg.virtualfs.core.ItemType
import java.util.*

/**
 * Provides information about file.
 * @property name String
 * @property type ItemType
 * @property size Long
 * @property created Date
 * @property lastModified Date
 * @constructor
 */
data class FileInfo(
    val name: String,
    val type: ItemType,
    val size: Long,
    val created: Date,
    val lastModified: Date)
