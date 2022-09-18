package utils

import Side
import java.math.BigDecimal
import java.math.RoundingMode

internal fun brokerage(
    broker: String,
    instrument: String,
    entry: BigDecimal,
    exit: BigDecimal,
    quantity: BigDecimal,
    side: Side,
): BigDecimal {

    val (sttMultiplier, excTransChargeMultiplier) = when (instrument.lowercase()) {
        "equity" -> "0.00025".toBigDecimal() to "0.0000345".toBigDecimal()
        "futures" -> "0.0001".toBigDecimal() to "0.00002".toBigDecimal()
        "options" -> "0.0005".toBigDecimal() to "0.00053".toBigDecimal()
        else -> error("Invalid instrument: $instrument")
    }

    val (buyPrice, sellPrice) = when (side) {
        Side.Long -> entry to exit
        Side.Short -> exit to entry
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

    val netProfit = (sellTurnover - buyTurnover - totalCharges).setScale(2, RoundingMode.HALF_EVEN)

    return netProfit
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
