package com.saurabhsandav.trading.broker

import com.saurabhsandav.trading.core.Instrument
import java.math.BigDecimal

interface Broker {

    val id: BrokerId

    val name: String

    fun calculateBrokerage(
        instrument: Instrument,
        entry: BigDecimal,
        exit: BigDecimal,
        quantity: BigDecimal,
        isLong: Boolean,
    ): Brokerage
}
