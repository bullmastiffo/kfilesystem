package com.mvg.virtualfs.core

import arrow.core.Either
import java.io.Closeable
import java.util.*

interface AttributeSet {
    val created: Date
    val lastModified: Date
}

interface ItemHandler : Closeable {
    val name: String
    val attributes: AttributeSet
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
    constructor(_name: String,descr: ItemDescriptor ): this(descr.nodeId, descr.type, descr.attributeSet, _name)
}


data class ItemTemplate(val name: String, val type: ItemType){
}

interface FileHandler: ItemHandler{

}

interface FolderHandler : ItemHandler {
    fun listItems(): Either<CoreFileSystemError, Iterable<NamedItemDescriptor>>
    fun getItem(name: String): Either<CoreFileSystemError, ItemHandler>
    fun createItem(item: ItemTemplate): ItemHandler
}
