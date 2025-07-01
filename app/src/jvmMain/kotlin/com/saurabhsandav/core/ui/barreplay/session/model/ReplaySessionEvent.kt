package com.saurabhsandav.core.ui.barreplay.session.model

import com.saurabhsandav.core.ui.stockchart.StockChart
import com.saurabhsandav.trading.backtest.BacktestOrderId

internal sealed class ReplaySessionEvent {

    data object AdvanceReplay : ReplaySessionEvent()

    data object AdvanceReplayByBar : ReplaySessionEvent()

    data class SetIsAutoNextEnabled(
        val isAutoNextEnabled: Boolean,
    ) : ReplaySessionEvent()

    data class Buy(
        val stockChart: StockChart,
    ) : ReplaySessionEvent()

    data class Sell(
        val stockChart: StockChart,
    ) : ReplaySessionEvent()

    data class CancelOrder(
        val id: BacktestOrderId,
    ) : ReplaySessionEvent()
}
