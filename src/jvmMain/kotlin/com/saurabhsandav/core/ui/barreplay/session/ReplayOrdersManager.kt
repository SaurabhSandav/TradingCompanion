package com.saurabhsandav.core.ui.barreplay.session

import androidx.compose.runtime.Stable
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.coroutines.binding.binding
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.Instrument
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.model.TradeExecutionSide
import com.saurabhsandav.core.trading.Candle
import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.MutableCandleSeries
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.trading.backtest.BacktestBroker
import com.saurabhsandav.core.trading.backtest.BacktestOrder.ClosedOrder
import com.saurabhsandav.core.trading.backtest.BacktestOrder.OrderParams
import com.saurabhsandav.core.trading.backtest.OrderExecutionType
import com.saurabhsandav.core.trading.backtest.newCandle
import com.saurabhsandav.core.trading.barreplay.BarReplay
import com.saurabhsandav.core.trading.barreplay.ReplaySeries
import com.saurabhsandav.core.trading.data.CandleRepository
import com.saurabhsandav.core.ui.barreplay.model.BarReplayState.ReplayParams
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.core.utils.PrefKeys
import com.saurabhsandav.core.utils.binarySearchByAsResult
import com.saurabhsandav.core.utils.indexOrNaturalIndex
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.math.BigDecimal
import kotlin.random.Random

@Stable
internal class ReplayOrdersManager(
    private val coroutineScope: CoroutineScope,
    private val replayParams: ReplayParams,
    private val barReplay: BarReplay,
    private val appPrefs: FlowSettings,
    private val tradingProfiles: TradingProfiles,
    private val candleRepo: CandleRepository,
) {

    private val replaySeriesAndScopeCache = mutableMapOf<String, ReplaySeriesAndScope>()
    private val backtestBroker = BacktestBroker()

    init {

        // Un-cache unused ReplaySeries
        backtestBroker.closedOrders
            .map { orders -> orders.map { it.params.ticker }.distinct() }
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

    val openOrders = backtestBroker.openOrders

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

            val replaySeries = createReplaySeries(stockChartParams.ticker)

            val replayProfileId = appPrefs.getLongOrNullFlow(PrefKeys.ReplayTradingProfile)
                .first()
                ?.let(::ProfileId)
                ?: error("Replay profile not set")

            val tradingRecord = tradingProfiles.getRecord(replayProfileId)

            val orderParams = OrderParams(
                broker = "Finvasia",
                instrument = Instrument.Equity,
                ticker = stockChartParams.ticker,
                quantity = quantity,
                lots = null,
                side = side,
            )

            val openOrder = backtestBroker.newOrder(
                instant = replaySeries.replayTime.value,
                params = orderParams,
                executionType = OrderExecutionType.Limit(price = price),
            )

            coroutineScope.launch contingentOrders@{

                val closedOrder = backtestBroker.closedOrders
                    .mapNotNull { closedOrders -> closedOrders.find { it.id == openOrder.id } }
                    .first()

                if (closedOrder !is ClosedOrder.Executed) return@contingentOrders

                val savedOrderId = tradingRecord.executions.new(
                    broker = closedOrder.params.broker,
                    instrument = closedOrder.params.instrument,
                    ticker = closedOrder.params.ticker,
                    quantity = closedOrder.params.quantity,
                    lots = closedOrder.params.lots,
                    side = closedOrder.params.side,
                    price = closedOrder.executionPrice,
                    timestamp = closedOrder.closedAt,
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

                    if (stop != null) {

                        // Add stop to trade stops
                        tradingRecord.trades.addStop(trade.id, stop)

                        // Send stop order to broker
                        val openStopOrder = backtestBroker.newOrder(
                            instant = replaySeries.replayTime.value,
                            params = positionCloseParams,
                            executionType = OrderExecutionType.StopMarket(trigger = stop),
                        )

                        // Handle stop order execution
                        coroutineScope.launch stopOrder@{

                            // Suspend until order is closed
                            val closedStopOrder = backtestBroker.closedOrders
                                .mapNotNull { closedOrders -> closedOrders.find { it.id == openStopOrder.id } }
                                .first()

                            // If order was executed, record it.
                            if (closedStopOrder is ClosedOrder.Executed) {

                                tradingRecord.executions.new(
                                    broker = closedStopOrder.params.broker,
                                    instrument = closedStopOrder.params.instrument,
                                    ticker = closedStopOrder.params.ticker,
                                    quantity = closedStopOrder.params.quantity,
                                    lots = closedStopOrder.params.lots,
                                    side = closedStopOrder.params.side,
                                    price = closedStopOrder.executionPrice,
                                    timestamp = closedStopOrder.closedAt,
                                    locked = false,
                                )
                            }
                        }
                    }

                    if (target != null) {

                        // Add target to trade targets
                        tradingRecord.trades.addTarget(trade.id, target)

                        // Send target order to broker
                        val openTargetOrder = backtestBroker.newOrder(
                            instant = replaySeries.replayTime.value,
                            params = positionCloseParams,
                            executionType = OrderExecutionType.Limit(price = target),
                        )

                        // Handle target order execution
                        coroutineScope.launch targetOrder@{

                            // Suspend until order is closed
                            val closedTargetOrder = backtestBroker.closedOrders
                                .mapNotNull { closedOrders -> closedOrders.find { it.id == openTargetOrder.id } }
                                .first()

                            // If order was executed, record it.
                            if (closedTargetOrder is ClosedOrder.Executed) {

                                tradingRecord.executions.new(
                                    broker = closedTargetOrder.params.broker,
                                    instrument = closedTargetOrder.params.instrument,
                                    ticker = closedTargetOrder.params.ticker,
                                    quantity = closedTargetOrder.params.quantity,
                                    lots = closedTargetOrder.params.lots,
                                    side = closedTargetOrder.params.side,
                                    price = closedTargetOrder.executionPrice,
                                    timestamp = closedTargetOrder.closedAt,
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

    fun cancelOrder(id: Long) {

        val orderToRemove = openOrders.value.find { it.id == id } ?: error("Replay order($id) not found")
        val replaySeries = replaySeriesAndScopeCache[orderToRemove.params.ticker]
            ?.replaySeries ?: error("ReplaySeries not found")

        backtestBroker.cancelOrder(
            instant = replaySeries.replayTime.value,
            openOrder = orderToRemove,
        )
    }

    private suspend fun createReplaySeries(ticker: String): ReplaySeries = replaySeriesAndScopeCache.getOrPut(ticker) {

        val candleSeries = getCandleSeries(ticker, replayParams.baseTimeframe)

        val replaySeries = barReplay.newSeries(
            inputSeries = candleSeries,
            initialIndex = candleSeries
                .binarySearchByAsResult(replayParams.replayFrom) { it.openInstant }
                .indexOrNaturalIndex,
        )

        val scope = MainScope()

        // Send price updates to BacktestBroker
        replaySeries.live
            .runningFold<Candle, Pair<Candle, Candle>?>(null) { accumulator, new ->
                (accumulator?.second ?: new) to new
            }
            .filterNotNull()
            .onEach { (prevCandle, newCandle) ->

                backtestBroker.newCandle(
                    ticker = ticker,
                    instant = replaySeries.replayTime.value,
                    prevCandle = prevCandle,
                    newCandle = newCandle,
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
