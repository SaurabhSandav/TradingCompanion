package com.saurabhsandav.trading.market.india

import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.kbigdecimal.KRoundingMode
import com.saurabhsandav.kbigdecimal.sumOf
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
        exchange: String,
        entry: KBigDecimal,
        exit: KBigDecimal,
        quantity: KBigDecimal,
        isLong: Boolean,
    ): Brokerage {

        val (buyPrice, sellPrice) = if (isLong) entry to exit else exit to entry
        val buyTurnover = (buyPrice * quantity).decimalPlaces(2, KRoundingMode.HalfEven)
        val sellTurnover = (sellPrice * quantity).decimalPlaces(2, KRoundingMode.HalfEven)

        // Assume just 2 orders - Buy and Sell
        val brokerage = when (instrument) {
            Instrument.Equity, Instrument.Futures -> {
                val brokerageBuy = (buyTurnover * "0.0003".toKBigDecimal()).coerceAtMost("20".toKBigDecimal())
                val brokerageSell = (sellTurnover * "0.0003".toKBigDecimal()).coerceAtMost("20".toKBigDecimal())
                brokerageBuy + brokerageSell
            }

            Instrument.Options -> (2 * 20).toKBigDecimal()
            Instrument.Index -> instrumentNotApplicableError(instrument)
        }

        val charges = indiaCharges(
            brokerage = brokerage,
            instrument = instrument,
            exchange = exchange,
            buyTurnover = buyTurnover,
            sellTurnover = sellTurnover,
        )

        val totalCharges = charges.values.sumOf { it }
        val pointsToBreakeven = totalCharges.div(quantity, 2, KRoundingMode.HalfEven)
        val breakeven = if (isLong) entry + pointsToBreakeven else entry - pointsToBreakeven

        return Brokerage(
            pnl = (sellTurnover - buyTurnover).decimalPlaces(2, KRoundingMode.HalfEven),
            breakeven = breakeven,
            pointsToBreakeven = pointsToBreakeven,
            totalCharges = totalCharges,
            charges = charges,
        )
    }

    override fun areSymbolsExpired(lastDownloadInstant: Instant): Boolean = false

    override suspend fun downloadSymbols(onSave: (List<Symbol>) -> Unit) = Unit

    companion object {
        val Id = BrokerId("Zerodha")
    }
}
