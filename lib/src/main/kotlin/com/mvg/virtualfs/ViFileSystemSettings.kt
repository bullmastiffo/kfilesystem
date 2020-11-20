package com.mvg.virtualfs

/**
 * Settings to instantiate @see ViFileSystem
 * @property size Long Total size in bytes.
 * @property blockSize BlockSize? Block size.
 * @constructor
 */
data class ViFileSystemSettings(val size: Long, val blockSize: BlockSize?)