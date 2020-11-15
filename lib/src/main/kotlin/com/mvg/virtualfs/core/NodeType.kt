package com.mvg.virtualfs.core

enum class NodeType(val type: Byte){
    None(0),
    Folder(1),
    File(2);

    companion object {
        fun fromByte(type: Byte): NodeType {
            return when(type){
                0.toByte() -> None
                1.toByte() -> Folder
                2.toByte() -> File
                else -> throw IllegalArgumentException("type byte must be within applicable range")
            }
        }
    }
}