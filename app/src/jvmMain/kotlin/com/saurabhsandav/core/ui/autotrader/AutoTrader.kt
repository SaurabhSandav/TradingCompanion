package com.saurabhsandav.core.ui.autotrader

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.coroutines.binding.binding
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.Instrument
import com.saurabhsandav.core.trades.model.TradeExecutionSide
import com.saurabhsandav.core.trading.Candle
import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.MutableCandleSeries
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.trading.backtest.BacktestBroker
import com.saurabhsandav.core.trading.backtest.BacktestOrder
import com.saurabhsandav.core.trading.backtest.OrderExecutionType
import com.saurabhsandav.core.trading.backtest.newCandle
import com.saurabhsandav.core.trading.barreplay.BarReplay
import com.saurabhsandav.core.trading.barreplay.CandleUpdateType
import com.saurabhsandav.core.trading.barreplay.ReplaySeries
import com.saurabhsandav.core.trading.data.CandleRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Instant
import java.math.BigDecimal

interface AutoTrader {

    suspend fun trade(
        tickers: List<String>,
        from: Instant,
        to: Instant,
        strategyBuilder: TradingStrategy.Builder,
    )
}

fun interface TradingStrategy {

    suspend fun onNewCandle(candle: Candle)

    interface Environment {

        val broker: BacktestBroker

        suspend fun getCandleSeries(timeframe: Timeframe): CandleSeries?

        suspend fun newOrder(
            quantity: BigDecimal,
            side: TradeExecutionSide,
            executionType: OrderExecutionType,
        )
    }

    interface Builder {

        val title: String

        fun build(env: Environment): TradingStrategy
    }
}

internal class ReplayAutoTrader(
    private val candleRepo: CandleRepository,
    private val tradingProfiles: TradingProfiles,
) : AutoTrader {

    private val baseTimeframe = Timeframe.M1

    override suspend fun trade(
        tickers: List<String>,
        from: Instant,
        to: Instant,
        strategyBuilder: TradingStrategy.Builder,
    ) {

        val barReplay = BarReplay(
            timeframe = baseTimeframe,
            from = from,
            candleUpdateType = CandleUpdateType.OHLC,
        )
        val broker = BacktestBroker()
        val replaySeriesCache = mutableMapOf<Pair<String, Timeframe>, ReplaySeries>()

        val profile = tradingProfiles.newProfile(
            name = "${strategyBuilder.title}_${from}_$to",
            description = "Auto Trader generated profile",
            isTraining = true,
        ).first()
        val record = tradingProfiles.getRecord(profile.id)

        suspend fun getReplaySeries(
            ticker: String,
            timeframe: Timeframe,
        ): ReplaySeries = replaySeriesCache.getOrPut(ticker to timeframe) {
            getReplaySeries(barReplay, ticker, timeframe, from, to)
        }

        coroutineScope {

            val jobs = tickers.map { ticker ->

                val env = TradingStrategyEnvironmentImpl(
                    ticker = ticker,
                    broker = broker,
                    getCandleSeries = { getReplaySeries(ticker, it) },
                )
                val strategy = strategyBuilder.build(env)

                val liveSeries = getReplaySeries(ticker, baseTimeframe)
                var prevCandle = liveSeries.last()

                launch(start = CoroutineStart.UNDISPATCHED) {

                    liveSeries.live.collect { candle ->

                        broker.newCandle(
                            ticker = ticker,
                            instant = candle.openInstant,
                            prevCandle = prevCandle,
                            newCandle = candle,
                            replayOHLC = false,
                        )

                        strategy.onNewCandle(candle)

                        prevCandle = candle
                    }
                }
            }

            while (barReplay.advance()) yield()

            jobs.forEach { it.cancelAndJoin() }
        }

        println(broker.openOrders.value.size)
        broker.closedOrders.value.filterIsInstance<BacktestOrder.ClosedOrder.Executed>().forEach { closedOrder ->

            record.executions.new(
                broker = "Finvasia",
                instrument = Instrument.Equity,
                ticker = closedOrder.params.ticker,
                quantity = closedOrder.params.quantity,
                lots = null,
                side = closedOrder.params.side,
                price = closedOrder.executionPrice,
                timestamp = closedOrder.closedAt,
                locked = true,
            )
        }
        println(broker.closedOrders.value.size)
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

        val allCandlesResult = binding {

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

        return when (allCandlesResult) {
            is Ok -> MutableCandleSeries(allCandlesResult.value, timeframe)
            is Err -> when (val error = allCandlesResult.error) {
                is CandleRepository.Error.AuthError -> error("AuthError")
                is CandleRepository.Error.UnknownError -> error(error.message)
            }
        }
    }
}

private class TradingStrategyEnvironmentImpl(
    private val ticker: String,
    override val broker: BacktestBroker,
    private val getCandleSeries: suspend (Timeframe) -> CandleSeries?,
) : TradingStrategy.Environment {

    override suspend fun getCandleSeries(timeframe: Timeframe): CandleSeries? {
        return getCandleSeries.invoke(timeframe)
    }

    override suspend fun newOrder(
        quantity: BigDecimal,
        side: TradeExecutionSide,
        executionType: OrderExecutionType,
    ) {

        broker.newOrder(
            instant = getCandleSeries(Timeframe.M1)!!.last().openInstant,
            params = BacktestOrder.Params(
                broker = "Finvasia",
                instrument = Instrument.Equity,
                ticker = ticker,
                quantity = quantity,
                lots = null,
                side = side,
            ),
            executionType = executionType,
        )
    }
}
