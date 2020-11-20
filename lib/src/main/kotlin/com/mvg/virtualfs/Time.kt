package com.mvg.virtualfs

import java.util.*

/**
 * Time provider.
 */
interface Time {
    fun now(): Date {
        return Date()
    }
}