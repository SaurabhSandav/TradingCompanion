package com.saurabhsandav.core.ui.trades.detail.model

import java.math.BigDecimal

internal sealed class TradeDetailEvent {

    data class AddStop(val price: BigDecimal) : TradeDetailEvent()

    data class DeleteStop(val price: BigDecimal) : TradeDetailEvent()

    data class AddTarget(val price: BigDecimal) : TradeDetailEvent()

    data class DeleteTarget(val price: BigDecimal) : TradeDetailEvent()

    data class AddNote(val note: String) : TradeDetailEvent()

    data class UpdateNote(val id: Long, val note: String) : TradeDetailEvent()

    data class DeleteNote(val id: Long) : TradeDetailEvent()
}
