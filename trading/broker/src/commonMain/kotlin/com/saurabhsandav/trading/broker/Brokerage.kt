package com.saurabhsandav.trading.broker

import com.saurabhsandav.kbigdecimal.KBigDecimal

data class Brokerage(
    val totalCharges: KBigDecimal,
    val pointsToBreakeven: KBigDecimal,
    val breakeven: KBigDecimal,
    val pnl: KBigDecimal,
    val netPNL: KBigDecimal,
)
