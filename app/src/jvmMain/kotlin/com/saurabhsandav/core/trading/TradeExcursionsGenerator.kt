package com.saurabhsandav.core.trading

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.get
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.trading.candledata.CandleRepository
import com.saurabhsandav.trading.core.Timeframe
import com.saurabhsandav.trading.record.Trade
import com.saurabhsandav.trading.record.TradeExcursions
import com.saurabhsandav.trading.record.TradeStop
import com.saurabhsandav.trading.record.TradeTarget
import com.saurabhsandav.trading.record.model.TradeSide
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.coroutines.CoroutineContext
import kotlin.time.Clock

internal class TradeExcursionsGenerator(
    private val coroutineContext: CoroutineContext,
    private val tradingProfiles: TradingProfiles,
    private val candleRepo: CandleRepository,
) {

    suspend fun generateExcursions() = withContext(coroutineContext) {

        // Suspend until logged in
        candleRepo.isLoggedIn().first { it }

        // Generate excursions
        tradingProfiles.allProfiles.first().forEach { profile ->

            Logger.d(DebugTag) { "Generating Excursions for profile - ${profile.name}" }

            val tradingRecord = tradingProfiles.getRecord(profile.id)

            val instant = Clock.System.todayIn(TimeZone.currentSystemDefault())
                .atTime(LocalTime(0, 0))
                .toInstant(TimeZone.currentSystemDefault())
            val tradesWithoutExcursions = tradingRecord.trades.getWithoutExcursionsBefore(instant).first()

            if (tradesWithoutExcursions.isEmpty()) {
                Logger.d(DebugTag) { "No Trades found for profile - ${profile.name}" }
            } else {

                val tradeIds = tradesWithoutExcursions.map { it.id }
                val stops = tradingRecord.stops.getPrimary(tradeIds).first()
                val targets = tradingRecord.targets.getPrimary(tradeIds).first()

                tradesWithoutExcursions.forEach { trade ->

                    val excursions = getExcursions(
                        trade = trade,
                        stop = stops.find { it.tradeId == trade.id },
                        target = targets.find { it.tradeId == trade.id },
                    )

                    if (excursions != null) {

                        Logger.d(DebugTag) { "Saving Excursions for Trade#(${trade.id})" }

                        // Save Excursions
                        tradingRecord.excursions.set(
                            id = excursions.tradeId,
                            tradeMfePrice = excursions.tradeMfePrice,
                            tradeMfePnl = excursions.tradeMfePnl,
                            tradeMaePrice = excursions.tradeMaePrice,
                            tradeMaePnl = excursions.tradeMaePnl,
                            sessionMfePrice = excursions.sessionMfePrice,
                            sessionMfePnl = excursions.sessionMfePnl,
                            sessionMaePrice = excursions.sessionMaePrice,
                            sessionMaePnl = excursions.sessionMaePnl,
                        )
                    }
                }

                Logger.d(DebugTag) { "Finished generating Excursions for profile - ${profile.name}" }
            }
        }
    }

    suspend fun getExcursions(
        profileTradeId: ProfileTradeId,
        stop: TradeStop?,
        target: TradeTarget?,
    ): TradeExcursions? = withContext(coroutineContext) {

        val trade = tradingProfiles.getRecord(profileTradeId.profileId)
            .trades
            .getById(profileTradeId.tradeId)
            .first()

        return@withContext getExcursions(trade, stop, target)
    }

    private suspend fun getExcursions(
        trade: Trade,
        stop: TradeStop?,
        target: TradeTarget?,
    ): TradeExcursions? = withContext(coroutineContext) {

        val exitInstant = trade.exitTimestamp ?: return@withContext null

        // TODO: End of session is currently hardcoded to end of day.
        //  Should be replaced with a proper customizable way to detect end of session.
        //  Any solution should also handle trades which are open across sessions.
        val endOfSessionInstant = exitInstant
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .plus(DatePeriod(days = 1))
            .atStartOfDayIn(TimeZone.currentSystemDefault())

        val candles = candleRepo.getCandles(
            symbolId = trade.symbolId,
            timeframe = Timeframe.M1,
            from = trade.entryTimestamp,
            to = endOfSessionInstant,
            includeFromCandle = true,
        ).get()?.first()

        if (candles.isNullOrEmpty()) {
            Logger.d(DebugTag) { "No candles found for Trade#(${trade.id})" }
            return@withContext null
        }

        val tradeCandles = candles.dropLastWhile { it.openInstant > exitInstant }

        // TODO: Add ability to have different square off times based on broker, instrument, exchange.
        //  Also, ignore if not an intra day trade.
        // Drop candles after square-off time
        val squareOffRange = LocalTime(hour = 15, minute = 21)..LocalTime(hour = 15, minute = 30)
        val sessionCandles = candles.dropLastWhile {
            it.openInstant.toLocalDateTime(TimeZone.currentSystemDefault()).time in squareOffRange
        }

        // Max favourable price in trade
        val tradeMfePrice = when (trade.side) {
            TradeSide.Long -> tradeCandles.maxOf { it.high }
            TradeSide.Short -> tradeCandles.minOf { it.low }
        }

        // Max unfavourable price in trade
        val tradeMaePrice = when (trade.side) {
            TradeSide.Long -> tradeCandles.minOf { it.low }
            TradeSide.Short -> tradeCandles.maxOf { it.high }
        }

        // Max favourable price before stop
        val sessionMfePrice = when (trade.side) {
            TradeSide.Long -> sessionCandles.takeWhile { stop == null || it.low > stop.price }.maxOfOrNull { it.high }
            TradeSide.Short -> sessionCandles.takeWhile { stop == null || it.high < stop.price }.minOfOrNull { it.low }
        } ?: tradeMfePrice

        // Max unfavourable price before target
        val sessionMaePrice = when (trade.side) {
            TradeSide.Long ->
                sessionCandles.takeWhile { target == null || it.high < target.price }
                    .minOfOrNull { it.low }

            TradeSide.Short -> sessionCandles.takeWhile { target == null || it.low > target.price }
                .maxOfOrNull { it.high }
        } ?: tradeMaePrice

        fun Trade.calculatePnl(price: KBigDecimal): KBigDecimal = when (side) {
            TradeSide.Long -> (price - averageEntry) * quantity
            TradeSide.Short -> (averageEntry - price) * quantity
        }

        return@withContext TradeExcursions(
            tradeId = trade.id,
            tradeMfePrice = tradeMfePrice,
            tradeMfePnl = trade.calculatePnl(tradeMfePrice),
            tradeMaePrice = tradeMaePrice,
            tradeMaePnl = trade.calculatePnl(tradeMaePrice),
            sessionMfePrice = sessionMfePrice,
            sessionMfePnl = trade.calculatePnl(sessionMfePrice),
            sessionMaePrice = sessionMaePrice,
            sessionMaePnl = trade.calculatePnl(sessionMaePrice),
        )
    }

    companion object {

        private const val DebugTag = "ExcursionsGeneration"
    }
}
