package com.saurabhsandav.core.ui.common.chart

import com.saurabhsandav.lightweightcharts.IChartApi
import com.saurabhsandav.lightweightcharts.ITimeScaleApi
import com.saurabhsandav.lightweightcharts.callbacks.LogicalRangeChangeEventHandler
import com.saurabhsandav.lightweightcharts.callbacks.MouseEventHandler
import com.saurabhsandav.lightweightcharts.data.LogicalRange
import com.saurabhsandav.lightweightcharts.data.MouseEventParams
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetIn

fun IChartApi.crosshairMove(): Flow<MouseEventParams> {
    return callbackFlow {

        val handler = MouseEventHandler(::trySend)

        subscribeCrosshairMove(handler)

        awaitClose { unsubscribeCrosshairMove(handler) }
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
