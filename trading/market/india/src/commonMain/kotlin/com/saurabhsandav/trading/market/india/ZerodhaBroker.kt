package com.saurabhsandav.trading.market.india

import com.saurabhsandav.trading.broker.Broker
import com.saurabhsandav.trading.broker.BrokerId
import com.saurabhsandav.trading.broker.Brokerage
import com.saurabhsandav.trading.core.Instrument
import java.math.BigDecimal

class ZerodhaBroker : Broker {

    override val id: BrokerId = Id

    override val name: String = "Zerodha"

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

        val brokerageBuy = when {
            (buyTurnover * "0.0003".toBigDecimal()) > "20".toBigDecimal() -> "20".toBigDecimal()
            else -> (buyTurnover * "0.0003".toBigDecimal())
        }

        val brokerageSell = when {
            (sellTurnover * "0.0003".toBigDecimal()) > "20".toBigDecimal() -> "20".toBigDecimal()
            else -> (sellTurnover * "0.0003".toBigDecimal())
        }

        brokerageBuy + brokerageSell
    }

    companion object {
        val Id = BrokerId("Zerodha")
    }
}
