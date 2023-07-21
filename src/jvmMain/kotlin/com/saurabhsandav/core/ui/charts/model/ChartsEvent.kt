package com.saurabhsandav.core.ui.charts.model

import kotlinx.datetime.Instant

internal sealed class ChartsEvent {

    data class OpenChart(
        val ticker: String,
        val start: Instant,
        val end: Instant?,
    ) : ChartsEvent()

    data object CandleFetchLoginCancelled : ChartsEvent()
}
