package com.saurabhsandav.core.utils

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.onSuccess
import kotlinx.coroutines.delay

suspend fun <V, E> retryIOResult(
    times: Int = Int.MAX_VALUE,
    initialDelay: Long = 100, // 0.1 second
    maxDelay: Long = 1000, // 1 second
    factor: Double = 2.0,
    block: suspend () -> Result<V, E>,
): Result<V, E> {

    var currentDelay = initialDelay

    repeat(times - 1) {

        val result = block()

        result.onSuccess { return result }

        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    }

    return block() // last attempt
}
