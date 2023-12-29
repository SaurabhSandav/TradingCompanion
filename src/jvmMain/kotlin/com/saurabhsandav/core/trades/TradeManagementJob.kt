package com.saurabhsandav.core.trades

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.get
import com.saurabhsandav.core.TradingProfile
import com.saurabhsandav.core.trades.model.TradeSide
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.trading.data.CandleRepository
import com.saurabhsandav.core.utils.StartupJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlinx.datetime.*
import java.math.BigDecimal
import kotlin.time.Duration.Companion.minutes

internal class TradeManagementJob(
    private val tradingProfiles: TradingProfiles,
    private val candleRepo: CandleRepository,
) : StartupJob {

    override suspend fun run() {

        // Don't start right away after app launch
        delay(1.minutes)

        // Generate excursions
        tradingProfiles.allProfiles.first().forEach { profile -> generateMfeAndMae(profile) }
    }

    private suspend fun generateMfeAndMae(
        profile: TradingProfile,
    ) = withContext(Dispatchers.IO) {

        Logger.d(DebugTag) { "Generating Excursions for profile - ${profile.name}" }

        val tradesRepo = tradingProfiles.getRecord(profile.id).trades

        val instant = Clock.System.todayIn(TimeZone.currentSystemDefault())
            .atTime(LocalTime(0, 0))
            .toInstant(TimeZone.currentSystemDefault())
        val trades = tradesRepo.getWithoutExcursionsBefore(instant).first()

        if (trades.isEmpty()) Logger.d(DebugTag) { "No Trades found for profile - ${profile.name}" }

        trades.forEach { trade ->

            val stop = tradesRepo.getPrimaryStop(trade.id).first()?.price
            val target = tradesRepo.getPrimaryTarget(trade.id).first()?.price
            val exitInstant = trade.exitTimestamp!!

            // TODO: End of session is currently hardcoded to end of day.
            //  Should be replaced with a proper customizable way to detect end of session.
            //  Any solution should also handle trades which are open across sessions.
            val endOfSessionInstant = exitInstant
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date
                .plus(DatePeriod(days = 1))
                .atStartOfDayIn(TimeZone.currentSystemDefault())

            val candles = candleRepo.getCandles(
                ticker = trade.ticker,
                timeframe = Timeframe.M1,
                from = trade.entryTimestamp,
                to = endOfSessionInstant,
                includeFromCandle = true,
            ).get()?.first()

            if (candles.isNullOrEmpty()) {
                Logger.d(DebugTag) { "No candles found for Trade#(${trade.id})" }
                return@forEach
            }

            val tradeCandles = candles.dropLastWhile { it.openInstant > exitInstant }

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
                TradeSide.Long -> candles.takeWhile { stop == null || it.low > stop }.maxOfOrNull { it.high }
                TradeSide.Short -> candles.takeWhile { stop == null || it.high < stop }.minOfOrNull { it.low }
            } ?: tradeMfePrice

            // Max unfavourable price before target
            val sessionMaePrice = when (trade.side) {
                TradeSide.Long -> candles.takeWhile { target == null || it.high < target }.minOfOrNull { it.low }
                TradeSide.Short -> candles.takeWhile { target == null || it.low > target }.maxOfOrNull { it.high }
            } ?: tradeMaePrice

            Logger.d(DebugTag) { "Saving Excursions for Trade#(${trade.id}) (Candles found - ${tradeCandles.size})" }

            fun Trade.calculatePnl(price: BigDecimal): BigDecimal = when (side) {
                TradeSide.Long -> (price - averageEntry) * quantity
                TradeSide.Short -> (averageEntry - price) * quantity
            }

            // Save Excursions
            tradesRepo.setExcursions(
                id = trade.id,
                tradeMfePrice = tradeMfePrice,
                tradeMfePnl = trade.calculatePnl(tradeMfePrice),
                tradeMaePrice = tradeMaePrice,
                tradeMaePnl = trade.calculatePnl(tradeMaePrice),
                sessionMfePrice = sessionMfePrice,
                sessionMfePnl = trade.calculatePnl(sessionMfePrice),
                sessionMaePrice = sessionMaePrice,
                sessionMaePnl = trade.calculatePnl(sessionMaePrice),
            )

            // This logic does not need to run uninterrupted. Let other coroutines a chance to run.
            yield()
        }

        Logger.d(DebugTag) { "Finished generating Excursions for profile - ${profile.name}" }
    }

    companion object {

        private const val DebugTag = "ExcursionsGeneration"
    }
}
