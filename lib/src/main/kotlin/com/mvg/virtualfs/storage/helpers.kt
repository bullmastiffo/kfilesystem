package com.mvg.virtualfs.storage

import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer

fun ceilDivide(number: Int, denominator: Int): Int{
    return number / denominator + if (number % denominator > 0) {1} else {0}
}

const val FIRST_BLOCK_OFFSET = 1024L

inline fun <reified T> Encoder.encodeInstance(value: T) {
    this.encodeSerializableValue(serializer(), value)
}

