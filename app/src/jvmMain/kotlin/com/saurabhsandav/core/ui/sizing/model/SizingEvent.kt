package com.saurabhsandav.core.ui.sizing.model

import com.saurabhsandav.trading.record.model.SizingTradeId

internal sealed class SizingEvent {

    data class AddTrade(
        val ticker: String,
    ) : SizingEvent()

    data class UpdateTradeEntry(
        val id: SizingTradeId,
        val entry: String,
    ) : SizingEvent()

    data class UpdateTradeStop(
        val id: SizingTradeId,
        val stop: String,
    ) : SizingEvent()

    data class RemoveTrade(
        val id: SizingTradeId,
    ) : SizingEvent()

    data class OpenLiveTrade(
        val id: SizingTradeId,
    ) : SizingEvent()
}
