package com.saurabhsandav.trading.broker

import com.saurabhsandav.trading.core.Instrument
import com.saurabhsandav.trading.core.SymbolId
import java.math.BigDecimal

data class Symbol(
    val id: SymbolId,
    val instrument: Instrument,
    val exchange: String,
    val ticker: String,
    val description: String?,
    val tickSize: BigDecimal?,
    val quantityMultiplier: BigDecimal?,
)
