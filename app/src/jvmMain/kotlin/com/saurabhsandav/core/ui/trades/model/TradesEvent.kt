package com.saurabhsandav.core.ui.trades.model

import com.saurabhsandav.core.trades.model.TradeFilter
import com.saurabhsandav.core.trades.model.TradeId

internal sealed class TradesEvent {

    data class OpenDetails(val id: TradeId) : TradesEvent()

    data class OpenChart(val id: TradeId) : TradesEvent()

    data class SetFocusModeEnabled(val isEnabled: Boolean) : TradesEvent()

    data class ApplyFilter(val tradeFilter: TradeFilter) : TradesEvent()

    data object NewExecution : TradesEvent()
}
