package com.saurabhsandav.trading.test

import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.kbigdecimal.KRoundingMode
import com.saurabhsandav.kbigdecimal.toKBigDecimal
import com.saurabhsandav.trading.broker.Broker
import com.saurabhsandav.trading.broker.BrokerId
import com.saurabhsandav.trading.broker.BrokerProvider
import com.saurabhsandav.trading.broker.Brokerage
import com.saurabhsandav.trading.broker.Symbol
import com.saurabhsandav.trading.core.Instrument
import kotlin.time.Instant

class TestBroker : Broker {

    override val id: BrokerId = Id

    override val name: String = "TestBroker"

    override fun calculateBrokerage(
        instrument: Instrument,
        entry: KBigDecimal,
        exit: KBigDecimal,
        quantity: KBigDecimal,
        isLong: Boolean,
    ): Brokerage {

        val (buyPrice, sellPrice) = if (isLong) entry to exit else exit to entry

        val buyTurnover = (buyPrice * quantity).decimalPlaces(2, KRoundingMode.HalfEven)
        val sellTurnover = (sellPrice * quantity).decimalPlaces(2, KRoundingMode.HalfEven)

        // 0.03% or Rs. 5.00 whichever is low
        val brokerageBuy = (buyTurnover * "0.0003".toKBigDecimal()).coerceAtMost("5".toKBigDecimal())
        val brokerageSell = (sellTurnover * "0.0003".toKBigDecimal()).coerceAtMost("5".toKBigDecimal())

        val totalCharges = brokerageBuy + brokerageSell

        val pointsToBreakeven = (totalCharges / quantity).decimalPlaces(2, KRoundingMode.HalfEven)

        val breakeven = if (isLong) entry + pointsToBreakeven else entry - pointsToBreakeven

        val netProfit = (sellTurnover - buyTurnover - totalCharges).decimalPlaces(2, KRoundingMode.HalfEven)

        return Brokerage(
            totalCharges = totalCharges,
            pointsToBreakeven = pointsToBreakeven,
            breakeven = breakeven,
            pnl = (sellTurnover - buyTurnover).decimalPlaces(2, KRoundingMode.HalfEven),
            netPNL = netProfit,
        )
    }

    override suspend fun downloadSymbols(lastDownloadInstant: Instant?): List<Symbol>? = null

    companion object {
        val Id = BrokerId("TestBroker")
    }
}

object TestBrokerProvider : BrokerProvider {

    override fun getBroker(id: BrokerId): Broker = TestBroker()
}
