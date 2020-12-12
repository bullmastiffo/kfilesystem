package com.mvg.virtualfs.core

import arrow.core.Either

abstract class FolderHandlerDecoratorBase(private val target: FolderHandler): FolderHandler {
    override fun listItems(): Either<CoreFileSystemError, List<NamedItemDescriptor>> {
        return target.listItems()
    }

    override fun getItem(name: String): Either<CoreFileSystemError, ItemHandler> {
        return target.getItem(name)
    }

    override fun createItem(item: ItemTemplate): Either<CoreFileSystemError, ItemHandler> {
        return target.createItem(item)
    }

    override fun deleteItem(name: String): Either<CoreFileSystemError, Unit> {
        return target.deleteItem(name)
    }

    override val descriptor: NamedItemDescriptor
        get() = target.descriptor

    override fun close() {
        target.close()
    }

    override fun delete(): Either<CoreFileSystemError, Unit> {
        return target.delete()
    }
}