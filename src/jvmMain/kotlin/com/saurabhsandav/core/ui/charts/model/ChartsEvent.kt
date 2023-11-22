package com.saurabhsandav.core.ui.charts.model

import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import kotlinx.datetime.Instant

internal sealed class ChartsEvent {

    data class OpenChart(
        val ticker: String,
        val start: Instant,
        val end: Instant?,
    ) : ChartsEvent()

    data class MarkTrades(val tradeIds: List<ProfileTradeId>) : ChartsEvent()

    data object CandleDataLoginConfirmed : ChartsEvent()

    data object CandleDataLoginDeclined : ChartsEvent()
}
