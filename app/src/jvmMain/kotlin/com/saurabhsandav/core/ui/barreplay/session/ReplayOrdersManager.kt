package com.saurabhsandav.core.ui.barreplay.session

import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.Instrument
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.model.TradeExecutionSide
import com.saurabhsandav.core.trading.backtest.*
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.math.BigDecimal
import java.util.*
import kotlin.random.Random

internal class ReplayOrdersManager(
    private val coroutineScope: CoroutineScope,
    profileId: ProfileId?,
    private val replaySeriesCache: ReplaySeriesCache,
    private val tradingProfiles: TradingProfiles,
) {

    private val tradingRecord = coroutineScope.async { profileId?.let { tradingProfiles.getRecord(it) } }

    private val tickerPriceScopeCache = mutableMapOf<String, CoroutineScope>()
    private val account = BacktestAccount(10_000.toBigDecimal())
    private val backtestBroker = BacktestBroker(account)
    val openOrders = backtestBroker.orders.map { orders ->
        @Suppress("UNCHECKED_CAST")
        orders.filter { it.status is BacktestOrder.Status.Open } as List<BacktestOrder<BacktestOrder.Status.Open>>
    }

    init {

        // Un-cache unused ReplaySeries
        backtestBroker.positions
            .map { positions -> positions.map { it.ticker }.toSet() }
            .onEach { openTickers ->

                (tickerPriceScopeCache.keys - openTickers).forEach { ticker ->
                    tickerPriceScopeCache.remove(ticker)?.cancel()
                    replaySeriesCache.releaseForOrdersManager(ticker)
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

            val tradingRecord = tradingRecord.await() ?: error("Replay profile not set")

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

    private suspend fun createReplaySeries(ticker: String) {

        val replaySeries = replaySeriesCache.getForOrdersManager(ticker)

        tickerPriceScopeCache.getOrPut(ticker) {

            val scope = MainScope()

            // Send initial price to BacktestBroker
            backtestBroker.newPrice(
                instant = replaySeries.last().openInstant,
                ticker = ticker,
                price = replaySeries.last().close,
            )

            // Send price updates to BacktestBroker
            replaySeries.live
                .onEach { (_, candle) ->

                    backtestBroker.newCandle(
                        ticker = ticker,
                        candle = candle,
                        replayOHLC = false,
                    )
                }
                .launchIn(scope)

            scope
        }
    }
}
