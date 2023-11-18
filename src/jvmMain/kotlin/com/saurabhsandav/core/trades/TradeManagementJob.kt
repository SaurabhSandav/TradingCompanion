package com.saurabhsandav.core.trades

import com.github.michaelbull.result.get
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
            generateMfeAndMae(
                tradesRepo = tradingProfiles.getRecord(profile.id).trades,
                candleRepo = candleRepo,
            )
        }
    }

    private suspend fun generateMfeAndMae(
        tradesRepo: TradesRepo,
        candleRepo: CandleRepository,
    ) = withContext(Dispatchers.IO) {

        val instant = Clock.System.todayIn(TimeZone.currentSystemDefault())
            .atTime(LocalTime(0, 0))
            .toInstant(TimeZone.currentSystemDefault())
        val trades = tradesRepo.getWithoutMfeAndMaeBefore(instant).first()

        trades.forEach { trade ->

            val entryInstant = trade.entryTimestamp
            val exitInstant = trade.exitTimestamp!!

            val tradeCandles = candleRepo.getCandles(
                ticker = trade.ticker,
                timeframe = Timeframe.M1,
                from = entryInstant,
                to = exitInstant,
                edgeCandlesInclusive = true,
            ).get().takeIf { it?.isNotEmpty() == true } ?: return@forEach

            val mfePrice = when (trade.side) {
                TradeSide.Long -> tradeCandles.maxOf { it.high }
                TradeSide.Short -> tradeCandles.minOf { it.low }
            }

            val maePrice = when (trade.side) {
                TradeSide.Long -> tradeCandles.minOf { it.low }
                TradeSide.Short -> tradeCandles.maxOf { it.high }
            }

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
}
