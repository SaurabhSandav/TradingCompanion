package com.saurabhsandav.core.ui.barreplay.session

import com.saurabhsandav.core.trading.ProfileId
import com.saurabhsandav.core.trading.TradingProfiles
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.trading.backtest.BacktestAccount
import com.saurabhsandav.trading.backtest.BacktestBroker
import com.saurabhsandav.trading.backtest.BacktestOrder
import com.saurabhsandav.trading.backtest.BacktestOrderId
import com.saurabhsandav.trading.backtest.Limit
import com.saurabhsandav.trading.backtest.StopMarket
import com.saurabhsandav.trading.backtest.newCandle
import com.saurabhsandav.trading.broker.BrokerId
import com.saurabhsandav.trading.core.Instrument
import com.saurabhsandav.trading.core.SymbolId
import com.saurabhsandav.trading.record.model.TradeExecutionSide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.math.BigDecimal
import kotlin.random.Random
import kotlin.uuid.Uuid

internal class ReplayOrdersManager(
    private val coroutineScope: CoroutineScope,
    profileId: ProfileId?,
    private val replaySeriesCache: ReplaySeriesCache,
    private val tradingProfiles: TradingProfiles,
) {

    private val tradingRecord = coroutineScope.async { profileId?.let { tradingProfiles.getRecord(it) } }

    private val symbolPriceScopeCache = mutableMapOf<SymbolId, CoroutineScope>()
    private val account = BacktestAccount(10_000.toBigDecimal())
    private val backtestBroker = BacktestBroker(account, tradingProfiles.brokerProvider)
    val openOrders = backtestBroker.orders.map { orders ->
        @Suppress("UNCHECKED_CAST")
        orders.filter { it.status is BacktestOrder.Status.Open } as List<BacktestOrder<BacktestOrder.Status.Open>>
    }

    init {

        // Un-cache unused ReplaySeries
        backtestBroker.positions
            .map { positions -> positions.map { it.symbolId }.toSet() }
            .onEach { openSymbolIds ->

                (symbolPriceScopeCache.keys - openSymbolIds).forEach { symbolId ->
                    symbolPriceScopeCache.remove(symbolId)?.cancel()
                    replaySeriesCache.releaseForOrdersManager(symbolId)
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

            // Updates broker with price for symbol
            createReplaySeries(stockChartParams.symbolId)

            val tradingRecord = tradingRecord.await() ?: error("Replay profile not set")

            val orderParams = BacktestOrder.Params(
                brokerId = BrokerId("Finvasia"),
                instrument = Instrument.Equity,
                symbolId = stockChartParams.symbolId,
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

                val status = closedOrder.status

                if (status !is BacktestOrder.Status.Executed) return@contingentOrders

                val savedOrderId = tradingRecord.executions.new(
                    brokerId = closedOrder.params.brokerId,
                    instrument = closedOrder.params.instrument,
                    symbolId = closedOrder.params.symbolId,
                    quantity = closedOrder.params.quantity,
                    lots = closedOrder.params.lots,
                    side = closedOrder.params.side,
                    price = status.executionPrice,
                    timestamp = status.closedAt,
                    locked = false,
                )

                if (target != null || stop != null) {

                    // Find generated trade for executed order
                    val trade = tradingRecord.trades.getForExecution(savedOrderId).first().single { !it.isClosed }

                    val positionCloseParams = orderParams.copy(
                        side = when (orderParams.side) {
                            TradeExecutionSide.Buy -> TradeExecutionSide.Sell
                            TradeExecutionSide.Sell -> TradeExecutionSide.Buy
                        },
                    )

                    val exitOcoId = Uuid.random()

                    if (stop != null) {

                        // Add stop to trade stops
                        tradingRecord.stops.add(trade.id, stop)

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

                            val status = closedStopOrder.status

                            // If order was executed, record it.
                            if (status is BacktestOrder.Status.Executed) {

                                tradingRecord.executions.new(
                                    brokerId = closedStopOrder.params.brokerId,
                                    instrument = closedStopOrder.params.instrument,
                                    symbolId = closedStopOrder.params.symbolId,
                                    quantity = closedStopOrder.params.quantity,
                                    lots = closedStopOrder.params.lots,
                                    side = closedStopOrder.params.side,
                                    price = status.executionPrice,
                                    timestamp = status.closedAt,
                                    locked = false,
                                )
                            }
                        }
                    }

                    if (target != null) {

                        // Add target to trade targets
                        tradingRecord.targets.add(trade.id, target)

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
                                    orders.find {
                                        it.status is BacktestOrder.Status.Closed && it.id == openTargetOrderId
                                    }
                                }
                                .first()

                            val status = closedTargetOrder.status

                            // If order was executed, record it.
                            if (status is BacktestOrder.Status.Executed) {

                                tradingRecord.executions.new(
                                    brokerId = closedTargetOrder.params.brokerId,
                                    instrument = closedTargetOrder.params.instrument,
                                    symbolId = closedTargetOrder.params.symbolId,
                                    quantity = closedTargetOrder.params.quantity,
                                    lots = closedTargetOrder.params.lots,
                                    side = closedTargetOrder.params.side,
                                    price = status.executionPrice,
                                    timestamp = status.closedAt,
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

    private suspend fun createReplaySeries(symbolId: SymbolId) {

        val replaySeries = replaySeriesCache.getForOrdersManager(symbolId)

        symbolPriceScopeCache.getOrPut(symbolId) {

            val scope = MainScope()

            // Send initial price to BacktestBroker
            backtestBroker.newPrice(
                instant = replaySeries.last().openInstant,
                symbolId = symbolId,
                price = replaySeries.last().close,
            )

            // Send price updates to BacktestBroker
            replaySeries.live
                .onEach { (_, candle) ->

                    backtestBroker.newCandle(
                        symbolId = symbolId,
                        candle = candle,
                        replayOHLC = false,
                    )
                }
                .launchIn(scope)

            scope
        }
    }
}
