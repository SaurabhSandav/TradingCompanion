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
import com.saurabhsandav.trading.core.SymbolId
import kotlinx.coroutines.delay
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
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

    override fun areSymbolsExpired(lastDownloadInstant: Instant): Boolean {
        return (Clock.System.now() - lastDownloadInstant) > SymbolExpiryPeriod
    }

    override suspend fun downloadSymbols(onSave: (List<Symbol>) -> Unit) {

        delay(SymbolFetchDelay)
        onSave(TestSymbols.slice(0..0))

        delay(SymbolFetchDelay)
        onSave(TestSymbols.slice(1..2))
    }

    companion object {

        val Id = BrokerId("TestBroker")

        val SymbolExpiryPeriod = 1.days
        val SymbolFetchDelay = 100.milliseconds

        val TestSymbols = listOf(
            Symbol(
                id = SymbolId("TestSymbol1"),
                instrument = Instrument.Equity,
                exchange = "TestExchange",
                ticker = "TestTicker",
                description = "",
                tickSize = null,
                quantityMultiplier = null,
            ),
            Symbol(
                id = SymbolId("TestSymbol2"),
                instrument = Instrument.Futures,
                exchange = "TestExchange",
                ticker = "TestSymbol2",
                description = "",
                tickSize = null,
                quantityMultiplier = null,
            ),
            Symbol(
                id = SymbolId("TestSymbol3"),
                instrument = Instrument.Options,
                exchange = "TestExchange",
                ticker = "TestSymbol3",
                description = "",
                tickSize = null,
                quantityMultiplier = null,
            ),
        )
    }
}

object TestBrokerProvider : BrokerProvider {

    override fun getAllIds(): List<BrokerId> = listOf(TestBroker.Id)

    override fun getBroker(id: BrokerId): Broker = TestBroker()
}
