package com.saurabhsandav.core.ui.common.chart

import com.saurabhsandav.lightweight_charts.IChartApi
import com.saurabhsandav.lightweight_charts.ITimeScaleApi
import com.saurabhsandav.lightweight_charts.callbacks.LogicalRangeChangeEventHandler
import com.saurabhsandav.lightweight_charts.callbacks.MouseEventHandler
import com.saurabhsandav.lightweight_charts.misc.LogicalRange
import com.saurabhsandav.lightweight_charts.misc.MouseEventParams
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
