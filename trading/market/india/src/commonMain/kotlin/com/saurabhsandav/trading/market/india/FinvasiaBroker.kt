package com.saurabhsandav.trading.market.india

import com.saurabhsandav.trading.broker.Broker
import com.saurabhsandav.trading.broker.BrokerId
import com.saurabhsandav.trading.broker.Brokerage
import com.saurabhsandav.trading.core.Instrument
import java.math.BigDecimal

class FinvasiaBroker : Broker {

    override val id: BrokerId = Id

    override val name: String = "Finvasia"

    override fun calculateBrokerage(
        instrument: Instrument,
        entry: BigDecimal,
        exit: BigDecimal,
        quantity: BigDecimal,
        isLong: Boolean,
    ): Brokerage = indiaBrokerage(
        instrument = instrument,
        entry = entry,
        exit = exit,
        quantity = quantity,
        isLong = isLong,
    ) { buyTurnover, sellTurnover ->

        val brokerageBuy = (buyTurnover * "0.0003".toBigDecimal()).coerceAtMost("5".toBigDecimal())
        val brokerageSell = (sellTurnover * "0.0003".toBigDecimal()).coerceAtMost("5".toBigDecimal())

        brokerageBuy + brokerageSell
    }

    companion object {
        val Id = BrokerId("Finvasia")
    }
}
