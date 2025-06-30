package com.saurabhsandav.core.ui.charts.model

import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import com.saurabhsandav.trading.core.SymbolId
import kotlin.time.Instant

internal sealed class ChartsEvent {

    data class OpenChart(
        val symbolId: SymbolId,
        val start: Instant,
        val end: Instant?,
    ) : ChartsEvent()

    data class MarkTrades(
        val tradeIds: List<ProfileTradeId>,
    ) : ChartsEvent()
}
