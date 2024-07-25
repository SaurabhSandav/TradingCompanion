package com.saurabhsandav.core.ui.charts

import com.saurabhsandav.core.ui.charts.model.ChartsEvent
import com.saurabhsandav.core.ui.charts.model.ChartsEvent.MarkTrades
import com.saurabhsandav.core.ui.charts.model.ChartsEvent.OpenChart
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.datetime.Instant

class ChartsHandle {

    private val _events = Channel<ChartsEvent>(Channel.UNLIMITED)
    internal val events = _events.receiveAsFlow()

    var markedTradeIds = emptyList<ProfileTradeId>()

    fun openTicker(
        ticker: String,
        start: Instant,
        end: Instant?,
    ) {
        _events.trySend(OpenChart(ticker = ticker, start = start, end = end))
    }

    fun setMarkedTrades(tradeIds: List<ProfileTradeId>) {
        _events.trySend(MarkTrades(tradeIds))
        markedTradeIds = tradeIds
    }
}
