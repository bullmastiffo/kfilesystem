package com.mvg.virtualfs.core

import arrow.core.Either
import java.io.Closeable
import java.nio.channels.SeekableByteChannel
import java.util.*

interface AttributeSet {
    val size: Long
    val created: Date
    val lastModified: Date
}

interface ItemHandler : Closeable {
    val descriptor: NamedItemDescriptor
    fun delete(): Either<CoreFileSystemError, Unit>
}

enum class ItemType{
    File,
    Folder
}
open class ItemDescriptor(
        val nodeId: Int,
        val type: ItemType,
        val attributeSet: AttributeSet)

class NamedItemDescriptor(
        _nodeId: Int,
        _type: ItemType,
        _attributeSet: AttributeSet,
        val name: String
): ItemDescriptor(
        _nodeId,
        _type,
        _attributeSet)
{
    constructor(_name: String, d: ItemDescriptor ): this(d.nodeId, d.type, d.attributeSet, _name)
}


data class ItemTemplate(val name: String, val type: ItemType){
}

interface FileHandler: ItemHandler{
    val size: Long
    fun getByteChannel() : Either<CoreFileSystemError, SeekableByteChannel>
}

interface FolderHandler : ItemHandler {
    fun listItems(): Either<CoreFileSystemError, List<NamedItemDescriptor>>
    fun getItem(name: String): Either<CoreFileSystemError, ItemHandler>
    fun createItem(item: ItemTemplate): Either<CoreFileSystemError, ItemHandler>
    fun deleteItem(name: String): Either<CoreFileSystemError, Unit>
}
