package com.saurabhsandav.core.ui.barreplay.session

import androidx.compose.runtime.Stable
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.coroutines.binding.binding
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.Instrument
import com.saurabhsandav.core.trades.model.TradeExecutionSide
import com.saurabhsandav.core.trading.*
import com.saurabhsandav.core.trading.backtest.BacktestBroker
import com.saurabhsandav.core.trading.backtest.BacktestOrder.ClosedOrder
import com.saurabhsandav.core.trading.backtest.BacktestOrder.OrderParams
import com.saurabhsandav.core.trading.backtest.OrderExecutionType
import com.saurabhsandav.core.trading.barreplay.BarReplay
import com.saurabhsandav.core.trading.barreplay.ReplaySeries
import com.saurabhsandav.core.trading.data.CandleRepository
import com.saurabhsandav.core.ui.barreplay.model.BarReplayState.ReplayParams
import com.saurabhsandav.core.utils.PrefKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import kotlin.random.Random

@Stable
internal class ReplayOrdersManager(
    private val coroutineScope: CoroutineScope,
    private val replayParams: ReplayParams,
    private val barReplay: BarReplay,
    private val appModule: AppModule,
    private val appPrefs: FlowSettings = appModule.appPrefs,
    private val tradingProfiles: TradingProfiles = appModule.tradingProfiles,
    private val candleRepo: CandleRepository = appModule.candleRepo,
) {

    private val replaySeriesCache = mutableMapOf<String, ReplaySeries>()
    private val backtestBroker = BacktestBroker(
        getCurrentTime = { ticker ->
            val replaySeries = replaySeriesCache[ticker] ?: error("ReplaySeries not found")
            replaySeries.replayTime.value.toLocalDateTime(TimeZone.currentSystemDefault())
        },
    )

    val openOrders = backtestBroker.openOrders

    fun newOrder(
        broker: String,
        instrument: Instrument,
        ticker: String,
        quantity: BigDecimal,
        lots: Int?,
        side: TradeExecutionSide,
        price: BigDecimal,
        stop: BigDecimal?,
        target: BigDecimal?,
    ): Long {

        val orderId = Random.nextLong()

        coroutineScope.launch {

            val replaySession = createReplaySession(ticker)

            val replayProfileId = appPrefs.getLongOrNullFlow(PrefKeys.ReplayTradingProfile).first()
                ?: error("Replay profile not set")

            val tradingRecord = tradingProfiles.getRecord(replayProfileId)

            val orderParams = OrderParams(
                broker = broker,
                instrument = instrument,
                ticker = ticker,
                quantity = quantity,
                lots = lots,
                side = side,
            )

            val openOrder = backtestBroker.newOrder(
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
                    timestamp = replaySession.replayTime.first().toLocalDateTime(TimeZone.currentSystemDefault()),
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
                                    timestamp = replaySession.replayTime.first()
                                        .toLocalDateTime(TimeZone.currentSystemDefault()),
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
                                    timestamp = replaySession.replayTime.first()
                                        .toLocalDateTime(TimeZone.currentSystemDefault()),
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
        backtestBroker.cancelOrder(orderToRemove)
    }

    private suspend fun createReplaySession(ticker: String): ReplaySeries = replaySeriesCache.getOrPut(ticker) {

        val candleSeries = getCandleSeries(ticker, replayParams.baseTimeframe)

        val replaySeries = barReplay.newSeries(
            inputSeries = candleSeries,
            initialIndex = candleSeries.indexOfFirst { it.openInstant >= replayParams.replayFrom },
        )

        replaySeries.live
            .runningFold<Candle, Pair<Candle, Candle>?>(null) { accumulator, new ->
                (accumulator?.second ?: new) to new
            }
            .filterNotNull()
            .onEach { (prevCandle, newCandle) ->

                when {
                    replayParams.replayFullBar -> {

                        val (extreme1, extreme2) = when {
                            newCandle.isLong -> newCandle.low to newCandle.high
                            else -> newCandle.low to newCandle.high
                        }

                        backtestBroker.newPrice(ticker, prevCandle.close, newCandle.open)
                        backtestBroker.newPrice(ticker, prevCandle.open, extreme1)
                        backtestBroker.newPrice(ticker, extreme1, extreme2)
                        backtestBroker.newPrice(ticker, extreme2, newCandle.close)
                    }

                    else -> backtestBroker.newPrice(ticker, prevCandle.close, newCandle.close)
                }
            }
            .launchIn(coroutineScope)

        replaySeries
    }

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
                ).bind()
            }

            val candlesAfter = async {
                candleRepo.getCandles(
                    ticker = ticker,
                    timeframe = timeframe,
                    from = replayParams.replayFrom,
                    to = replayParams.dataTo,
                    edgeCandlesInclusive = false,
                ).bind()
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
