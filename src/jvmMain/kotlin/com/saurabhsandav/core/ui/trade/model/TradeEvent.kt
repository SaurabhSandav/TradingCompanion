package com.saurabhsandav.core.ui.trade.model

import java.math.BigDecimal

internal sealed class TradeEvent {

    data object AddToTrade : TradeEvent()

    data object CloseTrade : TradeEvent()

    data class NewFromExistingExecution(val fromExecutionId: Long) : TradeEvent()

    data class EditExecution(val executionId: Long) : TradeEvent()

    data class LockExecution(val executionId: Long) : TradeEvent()

    data class DeleteExecution(val executionId: Long) : TradeEvent()

    data class AddStop(val price: BigDecimal) : TradeEvent()

    data class DeleteStop(val price: BigDecimal) : TradeEvent()

    data class AddTarget(val price: BigDecimal) : TradeEvent()

    data class DeleteTarget(val price: BigDecimal) : TradeEvent()

    data class AddNote(val note: String) : TradeEvent()

    data class UpdateNote(val id: Long, val note: String) : TradeEvent()

    data class DeleteNote(val id: Long) : TradeEvent()
}
