package com.saurabhsandav.trading.record.model

import java.math.BigDecimal

data class Account(
    val balance: BigDecimal,
    val balancePerTrade: BigDecimal,
    val leverage: BigDecimal,
    val riskAmount: BigDecimal,
)
