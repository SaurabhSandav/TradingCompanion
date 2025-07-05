package com.saurabhsandav.trading.test

import com.saurabhsandav.trading.broker.Broker
import com.saurabhsandav.trading.broker.BrokerId
import com.saurabhsandav.trading.broker.BrokerProvider
import com.saurabhsandav.trading.broker.Brokerage
import com.saurabhsandav.trading.broker.Symbol
import com.saurabhsandav.trading.core.Instrument
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.time.Instant

class TestBroker : Broker {

    override val id: BrokerId = Id

    override val name: String = "TestBroker"

    override fun calculateBrokerage(
        instrument: Instrument,
        entry: BigDecimal,
        exit: BigDecimal,
        quantity: BigDecimal,
        isLong: Boolean,
    ): Brokerage {

        val (buyPrice, sellPrice) = if (isLong) entry to exit else exit to entry

        val buyTurnover = (buyPrice * quantity).setScale(2, RoundingMode.HALF_EVEN)
        val sellTurnover = (sellPrice * quantity).setScale(2, RoundingMode.HALF_EVEN)

        // 0.03% or Rs. 5.00 whichever is low
        val brokerageBuy = (buyTurnover * "0.0003".toBigDecimal()).coerceAtMost("5".toBigDecimal())
        val brokerageSell = (sellTurnover * "0.0003".toBigDecimal()).coerceAtMost("5".toBigDecimal())

        val totalCharges = brokerageBuy + brokerageSell

        val pointsToBreakeven = (totalCharges / quantity).setScale(2, RoundingMode.HALF_EVEN)

        val breakeven = if (isLong) entry + pointsToBreakeven else entry - pointsToBreakeven

        val netProfit = (sellTurnover - buyTurnover - totalCharges).setScale(2, RoundingMode.HALF_EVEN)

        return Brokerage(
            totalCharges = totalCharges,
            pointsToBreakeven = pointsToBreakeven,
            breakeven = breakeven,
            pnl = (sellTurnover - buyTurnover).setScale(2, RoundingMode.HALF_EVEN),
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
