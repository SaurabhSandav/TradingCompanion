package com.saurabhsandav.core.ui.trades.model

import com.saurabhsandav.core.trades.model.TradeFilter
import com.saurabhsandav.core.trades.model.TradeId
import com.saurabhsandav.core.trades.model.TradeTagId

internal sealed class TradesEvent {

    data class OpenDetails(val id: TradeId) : TradesEvent()

    data class OpenChart(val ids: List<TradeId>) : TradesEvent()

    data class SetFocusModeEnabled(val isEnabled: Boolean) : TradesEvent()

    data class ApplyFilter(val tradeFilter: TradeFilter) : TradesEvent()

    data object NewExecution : TradesEvent()

    data class DeleteTrades(val ids: List<TradeId>) : TradesEvent()

    data class AddTag(
        val tradesIds: List<TradeId>,
        val tagId: TradeTagId,
    ) : TradesEvent()
}
