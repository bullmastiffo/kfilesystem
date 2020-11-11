package com.mvg.virtualfs

import java.util.*

interface Time {
    fun now(): Date {
        return Date()
    }
}