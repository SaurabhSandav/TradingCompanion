package com.saurabhsandav.trading.broker

import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.trading.core.Instrument
import com.saurabhsandav.trading.core.SymbolId

data class Symbol(
    val id: SymbolId,
    val instrument: Instrument,
    val exchange: String,
    val ticker: String,
    val description: String?,
    val tickSize: KBigDecimal?,
    val quantityMultiplier: KBigDecimal?,
)
