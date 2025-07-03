package com.saurabhsandav.trading.market.india

import com.saurabhsandav.trading.broker.Brokerage
import com.saurabhsandav.trading.core.Instrument
import java.math.BigDecimal
import java.math.RoundingMode

internal fun indiaBrokerage(
    instrument: Instrument,
    entry: BigDecimal,
    exit: BigDecimal,
    quantity: BigDecimal,
    isLong: Boolean,
    brokerage: (buyTurnover: BigDecimal, sellTurnover: BigDecimal) -> BigDecimal,
): Brokerage {

    val (sttMultiplier, excTransChargeMultiplier) = when (instrument) {
        Instrument.Equity -> "0.00025".toBigDecimal() to "0.0000345".toBigDecimal()
        Instrument.Futures -> "0.0001".toBigDecimal() to "0.00002".toBigDecimal()
        Instrument.Options -> "0.0005".toBigDecimal() to "0.00053".toBigDecimal()
    }

    val (buyPrice, sellPrice) = if (isLong) entry to exit else exit to entry

    val buyTurnover = (buyPrice * quantity).setScale(2, RoundingMode.HALF_EVEN)
    val sellTurnover = (sellPrice * quantity).setScale(2, RoundingMode.HALF_EVEN)

    val brokerage = brokerage(buyTurnover, sellTurnover)

    val turnover = (buyTurnover + sellTurnover).setScale(2, RoundingMode.HALF_EVEN)

    val sttTotal = (sellTurnover * sttMultiplier).setScale(0, RoundingMode.HALF_EVEN)

    val excTransCharge = (excTransChargeMultiplier * turnover).setScale(2, RoundingMode.HALF_EVEN)

    val sebiCharges = (turnover * "0.000001".toBigDecimal()).setScale(2, RoundingMode.HALF_EVEN)

    val stax = ("0.18".toBigDecimal() * (brokerage + excTransCharge + sebiCharges)).setScale(2, RoundingMode.HALF_EVEN)

    val stampCharges = (buyTurnover * "0.00003".toBigDecimal()).setScale(0, RoundingMode.HALF_EVEN)

    val totalCharges = brokerage + sttTotal + excTransCharge + stax + sebiCharges + stampCharges

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
