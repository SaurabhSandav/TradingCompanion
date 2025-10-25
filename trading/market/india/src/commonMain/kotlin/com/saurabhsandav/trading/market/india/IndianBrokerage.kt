package com.saurabhsandav.trading.market.india

import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.kbigdecimal.KRoundingMode
import com.saurabhsandav.kbigdecimal.toKBigDecimal
import com.saurabhsandav.trading.core.Instrument

internal fun indiaCharges(
    brokerage: KBigDecimal,
    instrument: Instrument,
    exchange: String,
    buyTurnover: KBigDecimal,
    sellTurnover: KBigDecimal,
): Map<String, KBigDecimal> {

    val totalTurnover = (buyTurnover + sellTurnover).decimalPlaces(2, KRoundingMode.HalfEven)

    val sttTotal = calculateSTT(
        instrument = instrument,
        sellTurnover = sellTurnover,
    )

    val exchangeCharges = calculateExchangeCharges(
        instrument = instrument,
        exchange = exchange,
        totalTurnover = totalTurnover,
    )
    val sebiCharges = (totalTurnover * SebiTurnoverFee.toKBigDecimal()).decimalPlaces(2, KRoundingMode.HalfEven)
    val gst = calculateGst(
        brokerage = brokerage,
        sebiCharges = sebiCharges,
        exchangeTransactionCharges = exchangeCharges,
    )
    val stampCharges = calculateStampCharges(
        instrument = instrument,
        buyTurnover = buyTurnover,
    )

    return mapOf(
        "Brokerage" to brokerage,
        "STT" to sttTotal,
        "Exchange Transaction Charges" to exchangeCharges,
        "SEBI Charges" to sebiCharges,
        "GST" to gst,
        "Stamp Duty Charges" to stampCharges,
    )
}

private fun calculateSTT(
    instrument: Instrument,
    sellTurnover: KBigDecimal,
): KBigDecimal {

    val sttMultiplier = when (instrument) {
        Instrument.Equity -> "0.00025".toKBigDecimal()
        Instrument.Futures -> "0.0002".toKBigDecimal()
        Instrument.Options -> "0.001".toKBigDecimal()
        Instrument.Index -> instrumentNotApplicableError(instrument)
    }

    return (sellTurnover * sttMultiplier).decimalPlaces(0, KRoundingMode.HalfEven)
}

private fun calculateExchangeCharges(
    instrument: Instrument,
    exchange: String,
    totalTurnover: KBigDecimal,
): KBigDecimal {

    val multiplier = when (exchange) {
        "NSE" -> when (instrument) {
            Instrument.Equity -> "0.0000297".toKBigDecimal()
            Instrument.Futures -> "0.0000173".toKBigDecimal()
            Instrument.Options -> "0.0003503".toKBigDecimal()
            Instrument.Index -> instrumentNotApplicableError(instrument)
        }

        "BSE" -> when (instrument) {
            Instrument.Equity -> "0.0000375".toKBigDecimal()
            Instrument.Futures -> KBigDecimal.Zero
            Instrument.Options -> throw NotImplementedError()
            Instrument.Index -> instrumentNotApplicableError(instrument)
        }

        else -> error("Unknown exchange: $exchange")
    }

    return (totalTurnover * multiplier).decimalPlaces(2, KRoundingMode.HalfEven)
}

private fun calculateGst(
    brokerage: KBigDecimal,
    sebiCharges: KBigDecimal,
    exchangeTransactionCharges: KBigDecimal,
): KBigDecimal {
    return (GstRate.toKBigDecimal() * (brokerage + sebiCharges + exchangeTransactionCharges))
        .decimalPlaces(2, KRoundingMode.HalfEven)
}

private fun calculateStampCharges(
    instrument: Instrument,
    buyTurnover: KBigDecimal,
): KBigDecimal {

    val chargesFraction = when (instrument) {
        Instrument.Index -> instrumentNotApplicableError(instrument)
        Instrument.Equity -> "0.00003"
        Instrument.Futures -> "0.00002"
        Instrument.Options -> "0.00003"
    }.toKBigDecimal()

    return (buyTurnover * chargesFraction).decimalPlaces(0, KRoundingMode.HalfEven)
}

fun instrumentNotApplicableError(instrument: Instrument): Nothing {
    error("Instrument ($instrument) not applicable")
}

// Sebi Turnover Fee -> ₹10/Crore
private const val SebiTurnoverFee = "0.000001"

// GST rate on (Brokerage + SEBI charges + transaction charges)
private const val GstRate = "0.18"
