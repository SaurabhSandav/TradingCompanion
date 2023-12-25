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
import kotlin.time.Duration.Companion.minutes

internal class TradeManagementJob(
    private val tradingProfiles: TradingProfiles,
    private val candleRepo: CandleRepository,
) : StartupJob {

    override suspend fun run() {

        // Don't start right away after app launch
        delay(1.minutes)

        tradingProfiles.allProfiles.first().forEach { profile ->
            generateMfeAndMae(profile)
        }
    }

    private suspend fun generateMfeAndMae(
        profile: TradingProfile,
    ) = withContext(Dispatchers.IO) {

        Logger.d(DebugTag) { "Generating MFE/MAE for profile - ${profile.name}" }

        val tradesRepo = tradingProfiles.getRecord(profile.id).trades

        val instant = Clock.System.todayIn(TimeZone.currentSystemDefault())
            .atTime(LocalTime(0, 0))
            .toInstant(TimeZone.currentSystemDefault())
        val trades = tradesRepo.getWithoutMfeAndMaeBefore(instant).first()

        if (trades.isEmpty()) Logger.d(DebugTag) { "No Trades found for profile - ${profile.name}" }

        trades.forEach { trade ->

            val entryInstant = trade.entryTimestamp
            val exitInstant = trade.exitTimestamp!!

            val tradeCandles = candleRepo.getCandles(
                ticker = trade.ticker,
                timeframe = Timeframe.M1,
                from = entryInstant,
                to = exitInstant,
                includeFromCandle = true,
            ).get()?.first()

            if (tradeCandles.isNullOrEmpty()) {
                Logger.d(DebugTag) { "No candles found for Trade#(${trade.id})" }
                return@forEach
            }

            val mfePrice = when (trade.side) {
                TradeSide.Long -> tradeCandles.maxOf { it.high }
                TradeSide.Short -> tradeCandles.minOf { it.low }
            }

            val maePrice = when (trade.side) {
                TradeSide.Long -> tradeCandles.minOf { it.low }
                TradeSide.Short -> tradeCandles.maxOf { it.high }
            }

            Logger.d(DebugTag) { "Saving MFE/MAE for Trade#(${trade.id}) (Candles found - ${tradeCandles.size})" }

            // Save MFE and MAE
            tradesRepo.setMfeAndMae(
                id = trade.id,
                mfePrice = mfePrice,
                mfePnl = when (trade.side) {
                    TradeSide.Long -> (mfePrice - trade.averageEntry) * trade.quantity
                    TradeSide.Short -> (trade.averageEntry - mfePrice) * trade.quantity
                },
                maePrice = maePrice,
                maePnl = when (trade.side) {
                    TradeSide.Long -> (maePrice - trade.averageEntry) * trade.quantity
                    TradeSide.Short -> (trade.averageEntry - maePrice) * trade.quantity
                },
            )

            // This logic does not need to run uninterrupted. Let other coroutines a chance to run.
            yield()
        }
    }

    companion object {

        private const val DebugTag = "MfeMaeGeneration"
    }
}
