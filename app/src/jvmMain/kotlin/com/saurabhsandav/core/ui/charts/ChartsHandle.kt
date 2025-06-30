package com.saurabhsandav.core.ui.charts

import com.saurabhsandav.core.ui.charts.model.ChartsEvent
import com.saurabhsandav.core.ui.charts.model.ChartsEvent.MarkTrades
import com.saurabhsandav.core.ui.charts.model.ChartsEvent.OpenChart
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import com.saurabhsandav.trading.core.SymbolId
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlin.time.Instant

class ChartsHandle {

    private val events = Channel<ChartsEvent>(Channel.UNLIMITED)
    internal val eventsFlow = events.receiveAsFlow()

    var markedTradeIds = emptyList<ProfileTradeId>()

    fun openSymbol(
        symbolId: SymbolId,
        start: Instant,
        end: Instant?,
    ) {
        events.trySend(OpenChart(symbolId = symbolId, start = start, end = end))
    }

    fun setMarkedTrades(tradeIds: List<ProfileTradeId>) {
        events.trySend(MarkTrades(tradeIds))
        markedTradeIds = tradeIds
    }
}
