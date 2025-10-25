package com.saurabhsandav.trading.broker

import com.saurabhsandav.kbigdecimal.KBigDecimal

data class Brokerage(
    val pnl: KBigDecimal,
    val breakeven: KBigDecimal,
    val pointsToBreakeven: KBigDecimal,
    val totalCharges: KBigDecimal,
    val charges: Map<String, KBigDecimal>,
) {

    val netPNL = pnl - totalCharges
}
