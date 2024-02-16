package com.saurabhsandav.core.trading.autotrader

import com.saurabhsandav.core.trades.TradingRecord
import com.saurabhsandav.core.trades.model.Instrument
import com.saurabhsandav.core.trades.model.TradeExecutionSide
import com.saurabhsandav.core.trades.model.TradeFilter
import com.saurabhsandav.core.trades.model.TradeSide
import com.saurabhsandav.core.trading.backtest.BacktestBroker
import com.saurabhsandav.core.trading.backtest.BacktestOrder
import com.saurabhsandav.core.trading.backtest.BacktestOrderId
import com.saurabhsandav.core.trading.backtest.Market
import com.saurabhsandav.core.trading.sizing.PositionSizer
import com.saurabhsandav.core.trading.sizing.PositionSizerScope
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.random.Random

internal class SignalsManager(
    private val ticker: String,
    private val orderTypeToExecutionType: OrderTypeToExecutionType,
    private val sizer: PositionSizer,
    private val broker: BacktestBroker,
    private val record: TradingRecord,
) {

    private val orderStates = mutableListOf<OrderState>()

    fun processSignal(signal: Signal) {

        val orders = broker.orders.value.filter { it.params.ticker == ticker }

        when (signal) {
            Signal.CancelAllEntryOrders -> {

                // Close all unfilled entry orders
                orderStates.filterIsInstance<OrderState.EntryPlaced>()
                    .filter { orderState ->
                        orders.find { it.id == orderState.entryOrderId }!!.status is BacktestOrder.Status.Open
                    }
                    .onEach { broker.cancelOrder(it.entryOrderId) }
                    .let { orderStates.removeAll(it) }
            }

            Signal.ExitAllPositions -> {

                broker.orders.value
                    .filter { it.status is BacktestOrder.Status.Open }
                    .forEach { order ->
                        @Suppress("UNCHECKED_CAST")
                        order as BacktestOrder<BacktestOrder.Status.Open>

                        broker.cancelOrder(order.id)
                    }

                broker.positions.value
                    .filter { it.ticker == ticker }
                    .forEach { position ->

                        broker.newOrder(
                            params = BacktestOrder.Params(
                                broker = "Finvasia",
                                instrument = Instrument.Equity,
                                ticker = ticker,
                                quantity = position.quantity,
                                lots = null,
                                side = when (position.side) {
                                    TradeSide.Long -> TradeExecutionSide.Sell
                                    TradeSide.Short -> TradeExecutionSide.Buy
                                },
                            ),
                            executionType = Market,
                        )
                    }
            }

            is Signal.Buy -> {

                val sizingScope = PositionSizerScope(
                    balance = broker.availableMargin,
                    entry = signal.price,
                    stop = signal.stop,
                    leverage = 5.toBigDecimal(),
                )

                val quantity = with(sizer) { sizingScope.size() }
                    ?.setScale(0, RoundingMode.DOWN)
                    ?: return

                if (quantity <= BigDecimal.ZERO) return

                val orderId = broker.newOrder(
                    params = BacktestOrder.Params(
                        broker = "Finvasia",
                        instrument = Instrument.Equity,
                        ticker = ticker,
                        quantity = quantity,
                        lots = null,
                        side = TradeExecutionSide.Buy,
                    ),
                    executionType = orderTypeToExecutionType.entry(signal.price),
                )

                orderStates += OrderState.EntryPlaced(
                    entryOrderId = orderId,
                    side = TradeSide.Long,
                    stop = signal.stop,
                    target = signal.target,
                )
            }

            is Signal.Sell -> {

                val sizingScope = PositionSizerScope(
                    balance = broker.availableMargin,
                    entry = signal.price,
                    stop = signal.stop,
                    leverage = 5.toBigDecimal(),
                )

                val quantity = with(sizer) { sizingScope.size() }
                    ?.setScale(0, RoundingMode.DOWN)
                    ?: return

                if (quantity <= BigDecimal.ZERO) return

                val orderId = broker.newOrder(
                    params = BacktestOrder.Params(
                        broker = "Finvasia",
                        instrument = Instrument.Equity,
                        ticker = ticker,
                        quantity = quantity,
                        lots = null,
                        side = TradeExecutionSide.Sell,
                    ),
                    executionType = orderTypeToExecutionType.entry(signal.price),
                )

                orderStates += OrderState.EntryPlaced(
                    entryOrderId = orderId,
                    side = TradeSide.Short,
                    stop = signal.stop,
                    target = signal.target,
                )
            }
        }
    }

    suspend fun onEvent() {

        val iterator = orderStates.listIterator()
        while (iterator.hasNext()) {

            when (val orderState = iterator.next()) {
                // If entry order was executed, place stop and target orders.
                is OrderState.EntryPlaced -> {

                    val entryOrder = broker.orders.value.find { it.id == orderState.entryOrderId }!!

                    when (entryOrder.status) {
                        is BacktestOrder.Status.Open -> return
                        is BacktestOrder.Status.Canceled, is BacktestOrder.Status.Rejected -> {
                            iterator.remove()
                            return
                        }

                        is BacktestOrder.Status.Executed -> Unit
                    }

                    val tradeId = record.trades.getFiltered(
                        filter = TradeFilter(
                            isClosed = false,
                            tickers = listOf(ticker),
                        ),
                    ).first().last().id

                    record.stops.add(tradeId, orderState.stop)
                    record.targets.add(tradeId, orderState.target)

                    val (stopOrderId, targetOrderId) = when (orderState.side) {
                        TradeSide.Long -> {

                            val ocoId = "${ticker}_${Random.nextLong()}"

                            val stopId = broker.newOrder(
                                params = BacktestOrder.Params(
                                    broker = "Finvasia",
                                    instrument = Instrument.Equity,
                                    ticker = ticker,
                                    quantity = entryOrder.params.quantity,
                                    lots = null,
                                    side = TradeExecutionSide.Sell,
                                ),
                                ocoId = ocoId,
                                executionType = orderTypeToExecutionType.stop(orderState.stop),
                            )

                            val targetId = broker.newOrder(
                                params = BacktestOrder.Params(
                                    broker = "Finvasia",
                                    instrument = Instrument.Equity,
                                    ticker = ticker,
                                    quantity = entryOrder.params.quantity,
                                    lots = null,
                                    side = TradeExecutionSide.Sell,
                                ),
                                ocoId = ocoId,
                                executionType = orderTypeToExecutionType.target(orderState.target),
                            )

                            stopId to targetId
                        }

                        TradeSide.Short -> {

                            val ocoId = "${ticker}_${Random.nextLong()}"

                            val stopId = broker.newOrder(
                                params = BacktestOrder.Params(
                                    broker = "Finvasia",
                                    instrument = Instrument.Equity,
                                    ticker = ticker,
                                    quantity = entryOrder.params.quantity,
                                    lots = null,
                                    side = TradeExecutionSide.Buy,
                                ),
                                ocoId = ocoId,
                                executionType = orderTypeToExecutionType.stop(orderState.stop),
                            )

                            val targetId = broker.newOrder(
                                params = BacktestOrder.Params(
                                    broker = "Finvasia",
                                    instrument = Instrument.Equity,
                                    ticker = ticker,
                                    quantity = entryOrder.params.quantity,
                                    lots = null,
                                    side = TradeExecutionSide.Buy,
                                ),
                                ocoId = ocoId,
                                executionType = orderTypeToExecutionType.target(orderState.target),
                            )

                            stopId to targetId
                        }
                    }

                    iterator.set(
                        OrderState.StopAndTargetPlaced(
                            stopOrderId = stopOrderId,
                            targetOrderId = targetOrderId,
                        ),
                    )
                }

                // If either of stop or target order were executed, cancel the other order.
                is OrderState.StopAndTargetPlaced -> {

                    val stopOrder = broker.orders.value.find { it.id == orderState.stopOrderId }!!
                    val targetOrder = broker.orders.value.find { it.id == orderState.targetOrderId }!!

                    when {
                        stopOrder.status is BacktestOrder.Status.Executed -> broker.cancelOrder(targetOrder.id)
                        targetOrder.status is BacktestOrder.Status.Executed -> broker.cancelOrder(stopOrder.id)
                    }

                    if (stopOrder.status is BacktestOrder.Status.Closed &&
                        targetOrder.status is BacktestOrder.Status.Closed
                    ) {
                        iterator.remove()
                    }
                }
            }
        }
    }

    private sealed class OrderState {

        data class EntryPlaced(
            val entryOrderId: BacktestOrderId,
            val side: TradeSide,
            val stop: BigDecimal,
            val target: BigDecimal,
        ) : OrderState()

        data class StopAndTargetPlaced(
            val stopOrderId: BacktestOrderId,
            val targetOrderId: BacktestOrderId,
        ) : OrderState()
    }
}
