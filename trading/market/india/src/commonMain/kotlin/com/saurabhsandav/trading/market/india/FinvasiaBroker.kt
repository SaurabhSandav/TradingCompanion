package com.saurabhsandav.trading.market.india

import com.saurabhsandav.fyersapi.FyersApi
import com.saurabhsandav.fyersapi.model.response.ExchangeInstrumentType
import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.kbigdecimal.toKBigDecimal
import com.saurabhsandav.trading.broker.Broker
import com.saurabhsandav.trading.broker.BrokerId
import com.saurabhsandav.trading.broker.Brokerage
import com.saurabhsandav.trading.broker.OptionType
import com.saurabhsandav.trading.broker.Symbol
import com.saurabhsandav.trading.core.Instrument
import com.saurabhsandav.trading.core.SymbolId
import kotlinx.coroutines.withContext
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atDate
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.coroutines.CoroutineContext
import kotlin.time.Clock
import kotlin.time.Instant

class FinvasiaBroker(
    private val coroutineContext: CoroutineContext,
    private val fyersApi: FyersApi,
) : Broker {

    override val id: BrokerId = Id

    override val name: String = "Finvasia"

    override fun calculateBrokerage(
        instrument: Instrument,
        entry: KBigDecimal,
        exit: KBigDecimal,
        quantity: KBigDecimal,
        isLong: Boolean,
    ): Brokerage = indiaBrokerage(
        instrument = instrument,
        entry = entry,
        exit = exit,
        quantity = quantity,
        isLong = isLong,
    ) { buyTurnover, sellTurnover ->

        val brokerageBuy = (buyTurnover * "0.0003".toKBigDecimal()).coerceAtMost("5".toKBigDecimal())
        val brokerageSell = (sellTurnover * "0.0003".toKBigDecimal()).coerceAtMost("5".toKBigDecimal())

        brokerageBuy + brokerageSell
    }

    override fun areSymbolsExpired(lastDownloadInstant: Instant): Boolean {

        // 8:30 AM
        val downloadTime = LocalTime(hour = 8, minute = 30)

        val tz = TimeZone.currentSystemDefault()
        val now = Clock.System.now()

        val todayDate = now.toLocalDateTime(tz).date
        val todayDownloadInstant = downloadTime.atDate(todayDate).toInstant(tz)

        val relevantDownloadInstant = when {
            // If the current time is before today's download time, then the relevant download time is yesterday.
            now < todayDownloadInstant -> todayDate.minus(DatePeriod(days = 1)).atTime(downloadTime).toInstant(tz)
            else -> todayDownloadInstant
        }

        return lastDownloadInstant < relevantDownloadInstant
    }

    override suspend fun downloadSymbols(onSave: (List<Symbol>) -> Unit): Unit = withContext(coroutineContext) {

        val symbolsProviders = listOf(
            fyersApi::getNseCapitalMarketSymbolsCsv,
            fyersApi::getBseCapitalMarketSymbolsCsv,
            fyersApi::getNseEquityDerivativeSymbolsCsv,
            fyersApi::getBseEquityDerivativeSymbolsCsv,
        )

        return@withContext symbolsProviders.forEach { getSymbols ->

            val symbols = getSymbols().mapNotNull { symbol ->

                val instrument = when (symbol.exchangeInstrumentType) {
                    ExchangeInstrumentType.INDEX -> Instrument.Index
                    ExchangeInstrumentType.EQ -> Instrument.Equity

                    ExchangeInstrumentType.FUTIDX,
                    ExchangeInstrumentType.FUTIVX,
                    ExchangeInstrumentType.FUTSTK,
                        -> Instrument.Futures

                    ExchangeInstrumentType.OPTIDX,
                    ExchangeInstrumentType.OPTSTK,
                        -> Instrument.Options

                    else -> return@mapNotNull null
                }

                Symbol(
                    id = SymbolId(symbol.symbolTicker),
                    brokerId = Id,
                    exchange = symbol.exchange.name,
                    exchangeToken = symbol.scripCode.toString(),
                    instrument = instrument,
                    ticker = symbol.underlyingSymbol,
                    tickSize = symbol.tickSize,
                    lotSize = symbol.minLotSize.toKBigDecimal(),
                    description = symbol.symbolDetails,
                    expiry = when (instrument) {
                        Instrument.Equity -> null
                        else -> symbol.expiryDate.toLongOrNull()?.let(Instant::fromEpochSeconds)
                    },
                    strikePrice = if (instrument == Instrument.Equity) null else symbol.strikePrice,
                    optionType = when (symbol.optionType) {
                        "CE" -> OptionType.Call
                        "PE" -> OptionType.Put
                        else -> null
                    },
                )
            }

            onSave(symbols)
        }
    }

    companion object {
        val Id = BrokerId("Finvasia")
    }

    object SymbolIds {

        val NIFTY = SymbolId("NSE:NIFTY50-INDEX")
    }
}
