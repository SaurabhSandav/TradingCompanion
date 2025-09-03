package com.saurabhsandav.trading.market.india

import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.kbigdecimal.toKBigDecimal
import com.saurabhsandav.trading.broker.Broker
import com.saurabhsandav.trading.broker.BrokerId
import com.saurabhsandav.trading.broker.Brokerage
import com.saurabhsandav.trading.broker.Symbol
import com.saurabhsandav.trading.core.Instrument
import kotlin.time.Instant

class ZerodhaBroker : Broker {

    override val id: BrokerId = Id

    override val name: String = "Zerodha"

    override fun calculateBrokerage(
        instrument: Instrument,
        entry: KBigDecimal,
        exit: KBigDecimal,
        quantity: KBigDecimal,
        isLong: Boolean,
    ): Brokerage = indiaBrokerage(
        instrument = instrument,
        entry = entry,
        exit = exit,
        quantity = quantity,
        isLong = isLong,
    ) { buyTurnover, sellTurnover ->

        val brokerageBuy = when {
            (buyTurnover * "0.0003".toKBigDecimal()) > "20".toKBigDecimal() -> "20".toKBigDecimal()
            else -> (buyTurnover * "0.0003".toKBigDecimal())
        }

        val brokerageSell = when {
            (sellTurnover * "0.0003".toKBigDecimal()) > "20".toKBigDecimal() -> "20".toKBigDecimal()
            else -> (sellTurnover * "0.0003".toKBigDecimal())
        }

        brokerageBuy + brokerageSell
    }

    override suspend fun downloadSymbols(lastDownloadInstant: Instant?): List<Symbol>? = null

    companion object {
        val Id = BrokerId("Zerodha")
    }
}
