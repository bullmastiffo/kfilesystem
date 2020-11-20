package com.mvg.virtualfs.storage

/**
 * Gets whole part of division, ceiling the result.
 * @param number Int
 * @param denominator Int
 * @return Int
 */
fun ceilDivide(number: Int, denominator: Int): Int{
    return number / denominator + if (number % denominator > 0) {1} else {0}
}

/**
 * Offset of first block group in virtual filesystem.
 */
const val FIRST_BLOCK_OFFSET = 1024L
