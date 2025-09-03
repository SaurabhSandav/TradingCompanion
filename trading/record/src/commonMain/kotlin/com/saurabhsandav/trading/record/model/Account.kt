package com.saurabhsandav.trading.record.model

import com.saurabhsandav.kbigdecimal.KBigDecimal

data class Account(
    val balance: KBigDecimal,
    val balancePerTrade: KBigDecimal,
    val leverage: KBigDecimal,
    val riskAmount: KBigDecimal,
)
