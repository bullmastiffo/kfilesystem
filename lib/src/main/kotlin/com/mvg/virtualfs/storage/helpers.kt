package com.mvg.virtualfs.storage

fun ceilDivide(number: Int, denominator: Int): Int{
    return number / denominator + if (number % denominator > 0) {1} else {0}
}

const val FIRST_BLOCK_OFFSET = 1024L
