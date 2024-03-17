package com.saurabhsandav.core.ui.barreplay.session

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.coroutines.binding.binding
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.Instrument
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.model.TradeExecutionSide
import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.MutableCandleSeries
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.trading.backtest.*
import com.saurabhsandav.core.trading.barreplay.BarReplay
import com.saurabhsandav.core.trading.barreplay.ReplaySeries
import com.saurabhsandav.core.trading.data.CandleRepository
import com.saurabhsandav.core.ui.barreplay.model.BarReplayState.ReplayParams
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.core.utils.PrefKeys
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.math.BigDecimal
import java.util.*
import kotlin.random.Random

internal class ReplayOrdersManager(
    private val coroutineScope: CoroutineScope,
    private val replayParams: ReplayParams,
    private val barReplay: BarReplay,
    private val appPrefs: FlowSettings,
    private val tradingProfiles: TradingProfiles,
    private val candleRepo: CandleRepository,
) {

    private val replaySeriesAndScopeCache = mutableMapOf<String, ReplaySeriesAndScope>()
    private val account = BacktestAccount(10_000.toBigDecimal())
    private val backtestBroker = BacktestBroker(account)
    val openOrders = backtestBroker.orders.map { orders ->
        @Suppress("UNCHECKED_CAST")
        orders.filter { it.status is BacktestOrder.Status.Open } as List<BacktestOrder<BacktestOrder.Status.Open>>
    }

    init {

        // Un-cache unused ReplaySeries
        backtestBroker.orders
            .map { orders ->
                orders
                    .filter { it.status is BacktestOrder.Status.Closed }
                    .map { it.params.ticker }
                    .distinct()
            }
            .onEach { tickers ->

                val tickersToUnCache = replaySeriesAndScopeCache.keys - tickers.toSet()

                tickersToUnCache.forEach { ticker ->
                    val cacheEntry = replaySeriesAndScopeCache[ticker]!!
                    barReplay.removeSeries(cacheEntry.replaySeries)
                    cacheEntry.scope.cancel()
                    replaySeriesAndScopeCache.remove(ticker)
                }
            }
            .launchIn(coroutineScope)
    }

    fun newOrder(
        stockChartParams: StockChartParams,
        quantity: BigDecimal,
        side: TradeExecutionSide,
        price: BigDecimal,
        stop: BigDecimal?,
        target: BigDecimal?,
    ): Long {

        val orderId = Random.nextLong()

        coroutineScope.launch {

            // Updates broker with price for ticker
            createReplaySeries(stockChartParams.ticker)

            val replayProfileId = appPrefs.getLongOrNullFlow(PrefKeys.ReplayTradingProfile)
                .first()
                ?.let(::ProfileId)
                ?: error("Replay profile not set")

            val tradingRecord = tradingProfiles.getRecord(replayProfileId)

            val orderParams = BacktestOrder.Params(
                broker = "Finvasia",
                instrument = Instrument.Equity,
                ticker = stockChartParams.ticker,
                quantity = quantity,
                lots = null,
                side = side,
            )

            val openOrderId = backtestBroker.newOrder(
                params = orderParams,
                executionType = Limit(price = price),
            )

            coroutineScope.launch contingentOrders@{

                val closedOrder = backtestBroker.orders
                    .mapNotNull { orders ->
                        orders.find { it.status is BacktestOrder.Status.Closed && it.id == openOrderId }
                    }
                    .first()

                if (closedOrder.status !is BacktestOrder.Status.Executed) return@contingentOrders

                val savedOrderId = tradingRecord.executions.new(
                    broker = closedOrder.params.broker,
                    instrument = closedOrder.params.instrument,
                    ticker = closedOrder.params.ticker,
                    quantity = closedOrder.params.quantity,
                    lots = closedOrder.params.lots,
                    side = closedOrder.params.side,
                    price = closedOrder.status.executionPrice,
                    timestamp = closedOrder.status.closedAt,
                    locked = false,
                )

                if (target != null || stop != null) {

                    // Find generated trade for executed order
                    val trade = tradingRecord.trades.getTradesForExecution(savedOrderId).first().single { !it.isClosed }

                    val positionCloseParams = orderParams.copy(
                        side = when (orderParams.side) {
                            TradeExecutionSide.Buy -> TradeExecutionSide.Sell
                            TradeExecutionSide.Sell -> TradeExecutionSide.Buy
                        }
                    )

                    val exitOcoId = UUID.randomUUID()

                    if (stop != null) {

                        // Add stop to trade stops
                        tradingRecord.trades.addStop(trade.id, stop)

                        // Send stop order to broker
                        val openStopOrderId = backtestBroker.newOrder(
                            params = positionCloseParams,
                            executionType = StopMarket(trigger = stop),
                            ocoId = exitOcoId,
                        )

                        // Handle stop order execution
                        coroutineScope.launch stopOrder@{

                            // Suspend until order is closed
                            val closedStopOrder = backtestBroker.orders
                                .mapNotNull { orders ->
                                    orders.find { it.status is BacktestOrder.Status.Closed && it.id == openStopOrderId }
                                }
                                .first()

                            // If order was executed, record it.
                            if (closedStopOrder.status is BacktestOrder.Status.Executed) {

                                tradingRecord.executions.new(
                                    broker = closedStopOrder.params.broker,
                                    instrument = closedStopOrder.params.instrument,
                                    ticker = closedStopOrder.params.ticker,
                                    quantity = closedStopOrder.params.quantity,
                                    lots = closedStopOrder.params.lots,
                                    side = closedStopOrder.params.side,
                                    price = closedStopOrder.status.executionPrice,
                                    timestamp = closedStopOrder.status.closedAt,
                                    locked = false,
                                )
                            }
                        }
                    }

                    if (target != null) {

                        // Add target to trade targets
                        tradingRecord.trades.addTarget(trade.id, target)

                        // Send target order to broker
                        val openTargetOrderId = backtestBroker.newOrder(
                            params = positionCloseParams,
                            executionType = Limit(price = target),
                            ocoId = exitOcoId,
                        )

                        // Handle target order execution
                        coroutineScope.launch targetOrder@{

                            // Suspend until order is closed
                            val closedTargetOrder = backtestBroker.orders
                                .mapNotNull { orders ->
                                    orders.find { it.status is BacktestOrder.Status.Closed && it.id == openTargetOrderId }
                                }
                                .first()

                            // If order was executed, record it.
                            if (closedTargetOrder.status is BacktestOrder.Status.Executed) {

                                tradingRecord.executions.new(
                                    broker = closedTargetOrder.params.broker,
                                    instrument = closedTargetOrder.params.instrument,
                                    ticker = closedTargetOrder.params.ticker,
                                    quantity = closedTargetOrder.params.quantity,
                                    lots = closedTargetOrder.params.lots,
                                    side = closedTargetOrder.params.side,
                                    price = closedTargetOrder.status.executionPrice,
                                    timestamp = closedTargetOrder.status.closedAt,
                                    locked = false,
                                )
                            }
                        }
                    }
                }
            }
        }

        return orderId
    }

    fun cancelOrder(id: BacktestOrderId) {
        backtestBroker.cancelOrder(id = id)
    }

    private suspend fun createReplaySeries(ticker: String): ReplaySeries = replaySeriesAndScopeCache.getOrPut(ticker) {

        val candleSeries = getCandleSeries(ticker, replayParams.baseTimeframe)

        val replaySeries = barReplay.newSeries(inputSeries = candleSeries)

        val scope = MainScope()

        // Send price updates to BacktestBroker
        replaySeries.live
            .onEach { (_, candle) ->

                backtestBroker.newCandle(
                    ticker = ticker,
                    candle = candle,
                    replayOHLC = replayParams.replayFullBar,
                )
            }
            .launchIn(scope)

        ReplaySeriesAndScope(replaySeries, scope)
    }.replaySeries

    private suspend fun getCandleSeries(
        ticker: String,
        timeframe: Timeframe,
    ): CandleSeries {

        val allCandlesResult = binding {

            val candlesBefore = async {
                candleRepo.getCandlesBefore(
                    ticker = ticker,
                    timeframe = timeframe,
                    at = replayParams.replayFrom,
                    count = replayParams.candlesBefore,
                    includeAt = true,
                ).bind().first()
            }

            val candlesAfter = async {
                candleRepo.getCandles(
                    ticker = ticker,
                    timeframe = timeframe,
                    from = replayParams.replayFrom,
                    to = replayParams.dataTo,
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

    private class ReplaySeriesAndScope(
        val replaySeries: ReplaySeries,
        val scope: CoroutineScope,
    )
}
