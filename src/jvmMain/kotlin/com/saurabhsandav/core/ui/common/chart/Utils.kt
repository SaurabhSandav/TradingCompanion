package com.saurabhsandav.core.ui.common.chart

import com.saurabhsandav.core.chart.IChartApi
import com.saurabhsandav.core.chart.callbacks.MouseEventHandler
import com.saurabhsandav.core.chart.misc.MouseEventParams
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow

fun IChartApi.crosshairMove(): Flow<MouseEventParams> {
    return callbackFlow {

        val handler = MouseEventHandler(::trySend)

        subscribeCrosshairMove(handler)

        awaitClose { unsubscribeCrosshairMove(handler) }
    }.buffer(Channel.CONFLATED)
}
