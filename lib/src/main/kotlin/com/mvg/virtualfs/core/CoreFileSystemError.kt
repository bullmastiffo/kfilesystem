package com.mvg.virtualfs.core

import java.io.IOException

sealed class CoreFileSystemError {
    object CantCreateMoreItemsError: CoreFileSystemError()
    object VolumeIsFullError: CoreFileSystemError()
    object FileSystemCorruptedError: CoreFileSystemError()
    object ItemClosedError: CoreFileSystemError()
    object ItemAlreadyOpenedError: CoreFileSystemError()
    object InvalidItemNameError: CoreFileSystemError()
    object ItemWithSameNameAlreadyExistsError: CoreFileSystemError()
    object CantDeleteNonEmptyFolderError: CoreFileSystemError()

    class UnderlyingIOExceptionError(val ex: IOException): CoreFileSystemError()

    class ItemNotFoundError(val fileName: String): CoreFileSystemError()
}