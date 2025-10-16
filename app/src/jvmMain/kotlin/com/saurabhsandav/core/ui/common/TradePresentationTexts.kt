package com.saurabhsandav.core.ui.common

import com.saurabhsandav.core.CachedSymbol
import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.trading.broker.BrokerId
import com.saurabhsandav.trading.broker.OptionType
import com.saurabhsandav.trading.core.Instrument
import com.saurabhsandav.trading.core.SymbolId
import com.saurabhsandav.trading.record.TradeDisplay
import com.saurabhsandav.trading.record.TradeExecutionDisplay
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import java.util.Locale
import kotlin.time.Instant

fun CachedSymbol.toSymbolTitle(): String = buildSymbolTitle(
    symbolId = id,
    instrument = instrument,
    exchange = exchange,
    ticker = ticker,
    expiry = expiry,
    strikePrice = strikePrice,
    optionType = optionType,
)

fun TradeExecutionDisplay.getSymbolTitle(): String = buildSymbolTitle(
    symbolId = symbolId,
    instrument = instrument,
    exchange = exchange,
    ticker = ticker,
    expiry = expiry,
    strikePrice = strikePrice,
    optionType = optionType,
)

fun TradeDisplay.getSymbolTitle(): String = buildSymbolTitle(
    symbolId = symbolId,
    instrument = instrument,
    exchange = exchange,
    ticker = ticker,
    expiry = expiry,
    strikePrice = strikePrice,
    optionType = optionType,
)

private fun buildSymbolTitle(
    symbolId: SymbolId,
    instrument: Instrument,
    exchange: String?,
    ticker: String?,
    expiry: Instant?,
    strikePrice: KBigDecimal?,
    optionType: OptionType?,
): String = buildString {

    if (ticker == null) {
        append(symbolId.value)
        return@buildString
    }

    append(ticker)

    if (instrument == Instrument.Futures || instrument == Instrument.Options) {

        append(' ')
        append(
            expiry!!
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date
                .format(DateFormat),
        )

        if (instrument == Instrument.Futures) append(" FUT")

        if (instrument == Instrument.Options) {

            append(' ')
            append(strikePrice!!)

            append(' ')
            append(
                when (optionType!!) {
                    OptionType.Call -> "CE"
                    OptionType.Put -> "PE"
                },
            )
        }
    }

    if (exchange != null) {
        append(" (")
        append(exchange)
        append(")")
    }
}

private val DateFormat = LocalDate.Format {
    day()
    char(' ')
    monthName(MonthNames.ENGLISH_ABBREVIATED)
    char(' ')
    year()
}

fun TradeExecutionDisplay.getBrokerTitle(): String = buildBrokerTitle(
    brokerId = brokerId,
    brokerName = brokerName,
    instrument = instrument,
)

fun TradeDisplay.getBrokerTitle(): String = buildBrokerTitle(
    brokerId = brokerId,
    brokerName = brokerName,
    instrument = instrument,
)

fun buildBrokerTitle(
    brokerId: BrokerId,
    brokerName: String?,
    instrument: Instrument,
): String = buildString {

    append(brokerName ?: brokerId.value)

    append(" (")
    append(
        instrument.strValue
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
    )
    append(")")
}

fun buildQuantityText(
    instrument: Instrument,
    quantity: KBigDecimal,
    lots: Int,
): String = buildString {
    append(quantity)
    if (instrument == Instrument.Equity) return@buildString
    append(" (")
    append(lots)
    append(" ")
    append(if (lots == 1) "lot" else "lots")
    append(")")
}
