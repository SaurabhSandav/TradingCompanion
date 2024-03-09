package com.saurabhsandav.trading.market.india

import com.saurabhsandav.trading.broker.Broker
import com.saurabhsandav.trading.broker.BrokerId
import com.saurabhsandav.trading.broker.Brokerage
import com.saurabhsandav.trading.broker.Symbol
import com.saurabhsandav.trading.core.Instrument
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.time.Instant

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

        val twenty = "20".toBigDecimal()

        val brokerageBuy = minOf(buyTurnover * "0.0003".toBigDecimal(), twenty)
            .setScale(buyTurnover.scale(), RoundingMode.HALF_EVEN)

        val brokerageSell = minOf(sellTurnover * "0.0003".toBigDecimal(), twenty)
            .setScale(sellTurnover.scale(), RoundingMode.HALF_EVEN)

        brokerageBuy + brokerageSell
    }

    override suspend fun downloadSymbols(lastDownloadInstant: Instant?): List<Symbol>? = null

    companion object {
        val Id = BrokerId("Zerodha")
    }
}
