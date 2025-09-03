package com.saurabhsandav.trading.market.india

import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.kbigdecimal.KRoundingMode
import com.saurabhsandav.kbigdecimal.toKBigDecimal
import com.saurabhsandav.trading.broker.Brokerage
import com.saurabhsandav.trading.core.Instrument

internal fun indiaBrokerage(
    instrument: Instrument,
    entry: KBigDecimal,
    exit: KBigDecimal,
    quantity: KBigDecimal,
    isLong: Boolean,
    brokerage: (buyTurnover: KBigDecimal, sellTurnover: KBigDecimal) -> KBigDecimal,
): Brokerage {

    val (sttMultiplier, excTransChargeMultiplier) = when (instrument) {
        Instrument.Equity -> "0.00025".toKBigDecimal() to "0.0000345".toKBigDecimal()
        Instrument.Futures -> "0.0001".toKBigDecimal() to "0.00002".toKBigDecimal()
        Instrument.Options -> "0.0005".toKBigDecimal() to "0.00053".toKBigDecimal()
        Instrument.Index -> error("Cannot calculate brokerage for index instrument")
    }

    val (buyPrice, sellPrice) = if (isLong) entry to exit else exit to entry

    val buyTurnover = (buyPrice * quantity).decimalPlaces(2, KRoundingMode.HalfEven)
    val sellTurnover = (sellPrice * quantity).decimalPlaces(2, KRoundingMode.HalfEven)

    val brokerage = brokerage(buyTurnover, sellTurnover)

    val turnover = (buyTurnover + sellTurnover).decimalPlaces(2, KRoundingMode.HalfEven)

    val sttTotal = (sellTurnover * sttMultiplier).decimalPlaces(0, KRoundingMode.HalfEven)

    val excTransCharge = (excTransChargeMultiplier * turnover).decimalPlaces(2, KRoundingMode.HalfEven)

    val sebiCharges = (turnover * "0.000001".toKBigDecimal()).decimalPlaces(2, KRoundingMode.HalfEven)

    val stax = ("0.18".toKBigDecimal() * (brokerage + excTransCharge + sebiCharges))
        .decimalPlaces(2, KRoundingMode.HalfEven)

    val stampCharges = (buyTurnover * "0.00003".toKBigDecimal()).decimalPlaces(0, KRoundingMode.HalfEven)

    val totalCharges = brokerage + sttTotal + excTransCharge + stax + sebiCharges + stampCharges

    val pointsToBreakeven = totalCharges.div(quantity, 2, KRoundingMode.HalfEven)

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
