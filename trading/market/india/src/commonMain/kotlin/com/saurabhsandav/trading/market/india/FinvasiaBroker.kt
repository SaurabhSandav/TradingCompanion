package com.saurabhsandav.trading.market.india

import com.saurabhsandav.fyersapi.FyersApi
import com.saurabhsandav.fyersapi.model.response.ExchangeInstrumentType
import com.saurabhsandav.trading.broker.Broker
import com.saurabhsandav.trading.broker.BrokerId
import com.saurabhsandav.trading.broker.Brokerage
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
import java.math.BigDecimal
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
        entry: BigDecimal,
        exit: BigDecimal,
        quantity: BigDecimal,
        isLong: Boolean,
    ): Brokerage = indiaBrokerage(
        instrument = instrument,
        entry = entry,
        exit = exit,
        quantity = quantity,
        isLong = isLong,
    ) { buyTurnover, sellTurnover ->

        val brokerageBuy = (buyTurnover * "0.0003".toBigDecimal()).coerceAtMost("5".toBigDecimal())
        val brokerageSell = (sellTurnover * "0.0003".toBigDecimal()).coerceAtMost("5".toBigDecimal())

        brokerageBuy + brokerageSell
    }

    override suspend fun downloadSymbols(lastDownloadInstant: Instant?): List<Symbol>? = withContext(coroutineContext) {

        if (lastDownloadInstant != null && !shouldDownloadNewSymbols(lastDownloadInstant)) return@withContext null

        val symbols = listOf(
            fyersApi.getNseCapitalMarketSymbols(),
            fyersApi.getBseCapitalMarketSymbols(),
            fyersApi.getNseEquityDerivativeSymbols(),
            fyersApi.getBseEquityDerivativeSymbols(),
        ).flatten()

        return@withContext symbols.mapNotNull { symbol ->

            val instrument = when (symbol.exInstType) {
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
                id = SymbolId(symbol.fyToken),
                instrument = instrument,
                exchange = symbol.exchange.name,
                ticker = symbol.exSymbol,
                description = symbol.symbolDesc,
                tickSize = symbol.tickSize,
                quantityMultiplier = symbol.qtyMultiplier,
            )
        }
    }

    fun shouldDownloadNewSymbols(lastDownloadInstant: Instant): Boolean {

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

    companion object {
        val Id = BrokerId("Finvasia")
    }
}
