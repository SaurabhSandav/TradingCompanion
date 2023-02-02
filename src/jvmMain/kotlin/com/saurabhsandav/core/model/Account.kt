package com.saurabhsandav.core.model

import java.math.BigDecimal

internal data class Account(
    val balance: BigDecimal,
    val balancePerTrade: BigDecimal,
    val leverage: BigDecimal,
    val riskAmount: BigDecimal,
)
