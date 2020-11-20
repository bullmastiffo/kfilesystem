package com.mvg.virtualfs

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

suspend fun massiveRun(times: Int, action: suspend (Int) -> Unit) {
    coroutineScope {
        repeat(times) {
            launch {
                action(it)
            }
        }
    }
}