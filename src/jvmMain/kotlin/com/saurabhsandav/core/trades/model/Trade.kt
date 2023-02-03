package com.saurabhsandav.core.trades.model

import kotlinx.datetime.LocalDateTime
import java.math.BigDecimal

internal data class Trade(
    val id: Long,
    val broker: String,
    val ticker: String,
    val instrument: String,
    val quantity: Int,
    val closedQuantity: Int,
    val lots: Int?,
    val side: TradeSide,
    val averageEntry: BigDecimal,
    val entryTimestamp: LocalDateTime,
    val averageExit: BigDecimal?,
    val exitTimestamp: LocalDateTime?,
    val pnl: BigDecimal,
    val fees: BigDecimal,
    val netPnl: BigDecimal,
    val isClosed: Boolean,
)
