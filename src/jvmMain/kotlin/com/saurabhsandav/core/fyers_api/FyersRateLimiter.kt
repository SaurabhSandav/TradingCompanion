package com.saurabhsandav.core.fyers_api

import kotlinx.coroutines.delay
import kotlinx.datetime.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal class FyersRateLimiter {

    private var trackedDay = today()
    private var requestsToday = 0

    private var trackedMinute = currentMinute()
    private var requestsInMinute = 0

    private var trackedSecond = currentSecond()
    private var requestsInSecond = 0

    suspend fun limit() {

        // Day (10000 per day)
        when (trackedDay) {
            // If tracked day is today, check if requests exceed daily rate limit
            today() -> if (requestsToday > 10000) error("Fyers: Rate Limit exceeded for the day")
            // Else, reset day tracking
            else -> {
                trackedDay = today()
                requestsToday = 0
            }
        }

        // Minutes (200 per minute)
        val currentMinute = currentMinute()
        val durationSinceTrackedMinute = currentMinute - trackedMinute

        if (durationSinceTrackedMinute <= 1.minutes) {
            // If tracked minute is still going, check if requests exceed minute rate limit
            if (requestsInMinute > 200) {
                // If exceeds, suspend for the rest of the minute
                delay(1.minutes - durationSinceTrackedMinute)
            }
        } else {
            // Else, reset minute tracking
            trackedMinute = currentMinute()
            requestsInMinute = 0
        }

        // Seconds (10 per second)
        val currentSecond = currentSecond()
        val durationSinceTrackedSecond = currentSecond - trackedSecond

        if (durationSinceTrackedSecond <= 1.seconds) {
            // If tracked second is still going, check if requests exceed minute rate limit
            if (requestsInSecond > 10) {
                // If exceeds, suspend for the rest of the second
                delay(1.seconds - durationSinceTrackedSecond)
            }
        } else {
            // Else, reset second tracking
            trackedSecond = currentSecond()
            requestsInSecond = 0
        }

        requestsToday++
        requestsInSecond++
        requestsInMinute++
    }

    private fun today(): LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    private fun currentMinute(): Instant = Clock.System.now()

    private fun currentSecond(): Instant = Clock.System.now()
}
