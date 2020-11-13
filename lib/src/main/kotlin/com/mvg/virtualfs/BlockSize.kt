package com.mvg.virtualfs

enum class BlockSize(val size: Int)
{
    Block1Kb(0x400), Block2Kb(0x800), Block4Kb(0x1000)
}