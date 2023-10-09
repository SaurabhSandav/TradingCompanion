package com.saurabhsandav.core.fyers_api

import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal class FyersRateLimiter {

    private var trackedMinute = Instant.DISTANT_PAST
    private var requestsInMinute = 0

    private var trackedSecond = Instant.DISTANT_PAST
    private var requestsInSecond = 0

    suspend fun limit() {

        // Minutes (200 per minute)
        val currentMinute = currentMinute()
        val durationSinceTrackedMinute = currentMinute - trackedMinute

        if (durationSinceTrackedMinute <= 1.minutes) {
            // If tracked minute is still going, check if requests exceed minute rate limit
            if (requestsInMinute >= 200) {
                // If exceeds, suspend for the rest of the minute
                delay(1.minutes - durationSinceTrackedMinute + 1.seconds)
            }
        } else {
            // Else, reset minute tracking
            trackedMinute = currentMinute
            requestsInMinute = 0
        }

        // Seconds (10 per second)
        val currentSecond = currentSecond()
        val durationSinceTrackedSecond = currentSecond - trackedSecond

        if (durationSinceTrackedSecond <= 1.seconds) {
            // If tracked second is still going, check if requests exceed minute rate limit
            if (requestsInSecond >= 10) {
                // If exceeds, suspend for the rest of the second
                delay(1.seconds - durationSinceTrackedSecond + 10.milliseconds)
            }
        } else {
            // Else, reset second tracking
            trackedSecond = currentSecond
            requestsInSecond = 0
        }

        requestsInSecond++
        requestsInMinute++
    }

    private fun currentMinute(): Instant = Clock.System.now()

    private fun currentSecond(): Instant = Clock.System.now()
}
