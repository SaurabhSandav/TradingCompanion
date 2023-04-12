package com.saurabhsandav.core.utils

import com.saurabhsandav.core.trades.model.Instrument
import com.saurabhsandav.core.trades.model.TradeSide
import java.math.BigDecimal
import java.math.RoundingMode

data class Brokerage(
    val totalCharges: BigDecimal,
    val pointsToBreakeven: BigDecimal,
    val breakeven: BigDecimal,
    val pnl: BigDecimal,
    val netPNL: BigDecimal,
)

internal fun brokerage(
    broker: String,
    instrument: Instrument,
    entry: BigDecimal,
    exit: BigDecimal,
    quantity: BigDecimal,
    side: TradeSide,
): Brokerage {

    val (sttMultiplier, excTransChargeMultiplier) = when (instrument) {
        Instrument.Equity -> "0.00025".toBigDecimal() to "0.0000345".toBigDecimal()
        Instrument.Futures -> "0.0001".toBigDecimal() to "0.00002".toBigDecimal()
        Instrument.Options -> "0.0005".toBigDecimal() to "0.00053".toBigDecimal()
    }

    val (buyPrice, sellPrice) = when (side) {
        TradeSide.Long -> entry to exit
        TradeSide.Short -> exit to entry
    }

    val buyTurnover = (buyPrice * quantity).setScale(2, RoundingMode.HALF_EVEN)
    val sellTurnover = (sellPrice * quantity).setScale(2, RoundingMode.HALF_EVEN)

    val brokerage = when (broker.lowercase()) {
        "zerodha" -> calculateBrokerageZerodha(buyTurnover, sellTurnover)
        "finvasia" -> BigDecimal.ZERO
        else -> error("Invalid broker")
    }

    val turnover = (buyTurnover + sellTurnover).setScale(2, RoundingMode.HALF_EVEN)

    val sttTotal = (sellTurnover * sttMultiplier).setScale(0, RoundingMode.HALF_EVEN)

    val excTransCharge = (excTransChargeMultiplier * turnover).setScale(2, RoundingMode.HALF_EVEN)

    val sebiCharges = (turnover * "0.000001".toBigDecimal()).setScale(2, RoundingMode.HALF_EVEN)

    val stax = ("0.18".toBigDecimal() * (brokerage + excTransCharge + sebiCharges)).setScale(2, RoundingMode.HALF_EVEN)

    val stampCharges = (buyTurnover * "0.00003".toBigDecimal()).setScale(0, RoundingMode.HALF_EVEN)

    val totalCharges = brokerage + sttTotal + excTransCharge + stax + sebiCharges + stampCharges

    val pointsToBreakeven = (totalCharges / quantity).setScale(2, RoundingMode.HALF_EVEN)

    val breakeven = when (side) {
        TradeSide.Long -> entry + pointsToBreakeven
        TradeSide.Short -> entry - pointsToBreakeven
    }

    val netProfit = (sellTurnover - buyTurnover - totalCharges).setScale(2, RoundingMode.HALF_EVEN)

    return Brokerage(
        totalCharges = totalCharges,
        pointsToBreakeven = pointsToBreakeven,
        breakeven = breakeven,
        pnl = (sellTurnover - buyTurnover).setScale(2, RoundingMode.HALF_EVEN),
        netPNL = netProfit,
    )
}

private fun calculateBrokerageZerodha(buyTurnover: BigDecimal, sellTurnover: BigDecimal): BigDecimal {

    val brokerageBuy = when {
        (buyTurnover * "0.0003".toBigDecimal()) > "20".toBigDecimal() -> "20".toBigDecimal()
        else -> (buyTurnover * "0.0003".toBigDecimal())
    }

    val brokerageSell = when {
        (sellTurnover * "0.0003".toBigDecimal()) > "20".toBigDecimal() -> "20".toBigDecimal()
        else -> (sellTurnover * "0.0003".toBigDecimal())
    }

    return brokerageBuy + brokerageSell
}
