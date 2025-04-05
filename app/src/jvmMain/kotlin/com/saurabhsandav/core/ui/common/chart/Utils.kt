package com.saurabhsandav.core.ui.common.chart

import com.saurabhsandav.lightweightcharts.IChartApi
import com.saurabhsandav.lightweightcharts.ITimeScaleApi
import com.saurabhsandav.lightweightcharts.callbacks.LogicalRangeChangeEventHandler
import com.saurabhsandav.lightweightcharts.callbacks.MouseEventHandler
import com.saurabhsandav.lightweightcharts.data.LogicalRange
import com.saurabhsandav.lightweightcharts.data.MouseEventParams
import com.saurabhsandav.lightweightcharts.data.Time
import com.saurabhsandav.lightweightcharts.data.Time.BusinessDay
import com.saurabhsandav.lightweightcharts.data.Time.ISOString
import com.saurabhsandav.lightweightcharts.data.Time.UTCTimestamp
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.offsetIn
import kotlin.time.Duration.Companion.seconds

fun IChartApi.crosshairMove(): Flow<MouseEventParams> {
    return callbackFlow {

        val handler = MouseEventHandler(::trySend)

        subscribeCrosshairMove(handler)

        awaitClose { unsubscribeCrosshairMove(handler) }
    }.buffer(Channel.CONFLATED)
}

fun IChartApi.clicks(): Flow<MouseEventParams> {
    return callbackFlow {

        val handler = MouseEventHandler(::trySend)

        subscribeClick(handler)

        awaitClose { unsubscribeClick(handler) }
    }.buffer(Channel.CONFLATED)
}

fun ITimeScaleApi.visibleLogicalRangeChange(): Flow<LogicalRange?> {
    return callbackFlow {

        val handler = LogicalRangeChangeEventHandler(::trySend)

        subscribeVisibleLogicalRangeChange(handler)

        awaitClose { unsubscribeVisibleLogicalRangeChange(handler) }
    }.buffer(Channel.CONFLATED)
}

/**
 * Chart messes with timezone. Workaround it by subtracting current timezone difference.
 */
fun Instant.offsetTimeForChart(): Long {
    val timeZoneOffset = offsetIn(TimeZone.currentSystemDefault()).totalSeconds
    return epochSeconds + timeZoneOffset
}

fun Time.toInstant(): Instant {

    val instant = when (this) {
        is UTCTimestamp -> Instant.fromEpochSeconds(value)
        is BusinessDay -> LocalDate(year = year, monthNumber = month, dayOfMonth = day).atStartOfDayIn(TimeZone.UTC)
        is ISOString -> LocalDate.parse(value).atStartOfDayIn(TimeZone.UTC)
    }

    val timeZoneOffset = instant.offsetIn(TimeZone.currentSystemDefault()).totalSeconds

    return instant - timeZoneOffset.seconds
}
