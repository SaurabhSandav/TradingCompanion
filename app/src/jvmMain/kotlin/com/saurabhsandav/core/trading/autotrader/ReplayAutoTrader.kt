package com.saurabhsandav.core.trading.autotrader

import com.github.michaelbull.result.coroutines.coroutineBinding
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.TradingRecord
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.MutableCandleSeries
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.trading.autotrader.impl.StrategyInitScopeImpl
import com.saurabhsandav.core.trading.backtest.BacktestAccount
import com.saurabhsandav.core.trading.backtest.BacktestBroker
import com.saurabhsandav.core.trading.barreplay.BarReplay
import com.saurabhsandav.core.trading.barreplay.CandleUpdateType
import com.saurabhsandav.core.trading.barreplay.ReplaySeries
import com.saurabhsandav.core.trading.data.CandleRepository
import com.saurabhsandav.core.trading.sizing.PositionSizer
import com.saurabhsandav.core.trading.sizing.accountRisk
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

internal class ReplayAutoTrader(
    private val candleRepo: CandleRepository,
    private val tradingProfiles: TradingProfiles,
) {

    private val baseTimeframe = Timeframe.M1

    suspend fun trade(
        tickers: List<String>,
        from: Instant,
        to: Instant,
        strategy: Strategy,
        orderTypeToExecutionType: OrderTypeToExecutionType = OrderTypeToExecutionType(),
        sizer: PositionSizer = PositionSizer.accountRisk(0.02.toBigDecimal()),
    ): TradingRecord {

        val barReplay = BarReplay(
            timeframe = baseTimeframe,
            from = from,
            candleUpdateType = CandleUpdateType.OHLC,
        )
        val account = BacktestAccount(10_000.toBigDecimal())
        val broker = BacktestBroker(
            account = account,
            leverage = 5.toBigDecimal(),
            minimumOrderValue = 500.toBigDecimal(),
            onMarginCall = {
                throw Exception("Margin Call!")
            },
        )
        val replaySeriesCache = mutableMapOf<Pair<String, Timeframe>, ReplaySeries>()

        val saveToProfile = true
        var profileId: ProfileId? = null

        val record = when {
            saveToProfile -> {

                val profile = tradingProfiles.newProfile(
                    name = "${strategy.title}_${from}_$to",
                    description = "Auto Trader generated profile",
                    isTraining = true,
                ).first()

                profileId = profile.id

                tradingProfiles.getRecord(profile.id)
            }

            else -> {

                val path = Path("/home/saurabh/Downloads/StrategyTest/${Clock.System.now().epochSeconds}")

                path.createDirectories()

                TradingRecord(recordPath = path.toString()) { _, _ ->
                }
            }
        }

        suspend fun getReplaySeries(
            ticker: String,
            timeframe: Timeframe,
        ): ReplaySeries = replaySeriesCache.getOrPut(ticker to timeframe) {
            getReplaySeries(barReplay, ticker, timeframe, from, to)
        }

        val tz = TimeZone.currentSystemDefault()

        try {

            coroutineScope {

                val runners = tickers.map { ticker ->

                    async {

                        val strategyScope = StrategyInitScopeImpl(
                            ticker = ticker,
                            getCandleSeries = ::getReplaySeries,
                        )

                        StrategyRunner(
                            m1Series = getReplaySeries(ticker, Timeframe.M1),
                            ticker = ticker,
                            broker = broker,
                            orderTypeToExecutionType = orderTypeToExecutionType,
                            sizer = sizer,
                            record = record,
                            strategyInstance = with(strategy) { strategyScope.init() },
                        )
                    }
                }.awaitAll()

                while (barReplay.advance()) {
                    runners.forEach { runner -> runner.onAdvance() }
                    print(barReplay.currentInstant.toLocalDateTime(tz))
                    print("\r")
                }
                println()
            }
        } catch (e: Exception) {
            profileId?.let { tradingProfiles.deleteProfile(it) }
            throw e
        }

        println(account.balance)
        println(account.transactions.value.size)
        println(account.transactions.value.take(5))
        println(broker.orders.value.size)

        return record
    }

    private suspend fun getReplaySeries(
        barReplay: BarReplay,
        ticker: String,
        timeframe: Timeframe,
        from: Instant,
        to: Instant,
    ): ReplaySeries {

        val candleSeries = getCandleSeries(ticker, baseTimeframe, from, to)

        return barReplay.newSeries(
            inputSeries = candleSeries,
            timeframeSeries = when (baseTimeframe) {
                timeframe -> null
                else -> getCandleSeries(ticker, timeframe, from, to)
            },
        )
    }

    private suspend fun getCandleSeries(
        ticker: String,
        timeframe: Timeframe,
        from: Instant,
        to: Instant,
    ): CandleSeries {

        val allCandlesResult = coroutineBinding {

            val candlesBefore = async {
                candleRepo.getCandlesBefore(
                    ticker = ticker,
                    timeframe = timeframe,
                    at = from,
                    count = 200,
                    includeAt = true,
                ).bind().first()
            }

            val candlesAfter = async {
                candleRepo.getCandles(
                    ticker = ticker,
                    timeframe = timeframe,
                    from = from,
                    to = to,
                    includeFromCandle = false,
                ).bind().first()
            }

            candlesBefore.await() + candlesAfter.await()
        }

        return when {
            allCandlesResult.isOk -> MutableCandleSeries(allCandlesResult.value, timeframe)
            else -> when (val error = allCandlesResult.error) {
                is CandleRepository.Error.AuthError -> error("AuthError")
                is CandleRepository.Error.UnknownError -> error(error.message)
            }
        }
    }
}
