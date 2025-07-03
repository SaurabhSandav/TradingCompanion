package com.saurabhsandav.trading.broker

import java.math.BigDecimal

data class Brokerage(
    val totalCharges: BigDecimal,
    val pointsToBreakeven: BigDecimal,
    val breakeven: BigDecimal,
    val pnl: BigDecimal,
    val netPNL: BigDecimal,
)
