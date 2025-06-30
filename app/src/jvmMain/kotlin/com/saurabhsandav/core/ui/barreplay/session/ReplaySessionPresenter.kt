package com.saurabhsandav.core.ui.barreplay.session

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.trading.TradingProfiles
import com.saurabhsandav.core.ui.barreplay.model.BarReplayState.ReplayParams
import com.saurabhsandav.core.ui.barreplay.session.model.ReplaySessionEvent
import com.saurabhsandav.core.ui.barreplay.session.model.ReplaySessionEvent.AdvanceReplay
import com.saurabhsandav.core.ui.barreplay.session.model.ReplaySessionEvent.AdvanceReplayByBar
import com.saurabhsandav.core.ui.barreplay.session.model.ReplaySessionEvent.Buy
import com.saurabhsandav.core.ui.barreplay.session.model.ReplaySessionEvent.CancelOrder
import com.saurabhsandav.core.ui.barreplay.session.model.ReplaySessionEvent.Sell
import com.saurabhsandav.core.ui.barreplay.session.model.ReplaySessionEvent.SetIsAutoNextEnabled
import com.saurabhsandav.core.ui.barreplay.session.model.ReplaySessionState
import com.saurabhsandav.core.ui.barreplay.session.model.ReplaySessionState.OrderFormParams
import com.saurabhsandav.core.ui.barreplay.session.model.ReplaySessionState.ReplayChartInfo
import com.saurabhsandav.core.ui.barreplay.session.model.ReplaySessionState.ReplayOrderListItem
import com.saurabhsandav.core.ui.barreplay.session.replayorderform.model.ReplayOrderFormModel
import com.saurabhsandav.core.ui.common.TradeDateTimeFormat
import com.saurabhsandav.core.ui.common.app.AppWindowsManager
import com.saurabhsandav.core.ui.stockchart.StockChart
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.core.ui.stockchart.data.LoadConfig
import com.saurabhsandav.core.utils.emitInto
import com.saurabhsandav.core.utils.launchUnit
import com.saurabhsandav.trading.backtest.BacktestOrderId
import com.saurabhsandav.trading.backtest.Limit
import com.saurabhsandav.trading.backtest.Market
import com.saurabhsandav.trading.backtest.StopLimit
import com.saurabhsandav.trading.backtest.StopMarket
import com.saurabhsandav.trading.backtest.TrailingStop
import com.saurabhsandav.trading.barreplay.BarReplay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import java.util.Locale
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

internal class ReplaySessionPresenter(
    private val coroutineScope: CoroutineScope,
    private val replayParams: ReplayParams,
    stockChartsStateFactory: StockChartsStateFactory,
    private val barReplay: BarReplay,
    val replayOrdersManager: ReplayOrdersManager,
    private val tradingProfiles: TradingProfiles,
) {

    private var autoNextJob by mutableStateOf<Job?>(null)
    private val chartsState = stockChartsStateFactory(
        initialParams = StockChartParams(replayParams.initialSymbolId, replayParams.baseTimeframe),
        loadConfig = LoadConfig(initialLoadBefore = { replayParams.replayFrom }),
    )
    private val orderFormWindowsManager = AppWindowsManager<OrderFormParams>()

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule ReplaySessionState(
            chartsState = chartsState,
            profileName = getProfileName(),
            replayOrderItems = getReplayOrderItems().value,
            orderFormWindowsManager = orderFormWindowsManager,
            chartInfo = ::getChartInfo,
            isAutoNextEnabled = autoNextJob != null,
            eventSink = ::onEvent,
        )
    }

    private fun onEvent(event: ReplaySessionEvent) {

        when (event) {
            AdvanceReplay -> onAdvanceReplay()
            AdvanceReplayByBar -> onAdvanceReplayByBar()
            is SetIsAutoNextEnabled -> onSetIsAutoNextEnabled(event.isAutoNextEnabled)
            is Buy -> onBuy(event.stockChart)
            is Sell -> onSell(event.stockChart)
            is CancelOrder -> onCancelOrder(event.id)
        }
    }

    @Composable
    private fun getProfileName(): String? {
        return remember {
            when (replayParams.profileId) {
                null -> flowOf(null)
                else -> tradingProfiles.getProfileOrNull(replayParams.profileId).map { it?.name }
            }
        }.collectAsState(replayParams.profileId?.let { "" }).value
    }

    @Composable
    private fun getReplayOrderItems(): State<List<ReplayOrderListItem>> {
        return remember {
            replayOrdersManager.openOrders.map { openOrders ->
                openOrders.map { openOrder ->

                    val params = openOrder.params
                    val quantity = params.quantity

                    ReplayOrderListItem(
                        id = openOrder.id,
                        executionType = when (openOrder.executionType) {
                            is Limit -> "Limit"
                            is Market -> "Market"
                            is StopLimit -> "Stop Limit"
                            is StopMarket -> "Stop Market"
                            is TrailingStop -> "Trailing Stop"
                        },
                        broker = run {
                            val instrumentCapitalized = params.instrument.strValue
                                .replaceFirstChar {
                                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                                }
                            "${params.brokerId.value} ($instrumentCapitalized)"
                        },
                        ticker = params.symbolId.value,
                        quantity = params.lots
                            ?.let { "$quantity ($it ${if (it == 1) "lot" else "lots"})" } ?: quantity.toString(),
                        side = params.side.strValue.uppercase(),
                        price = when (val executionType = openOrder.executionType) {
                            is Limit -> executionType.price.toPlainString()
                            is Market -> ""
                            is StopLimit -> executionType.price.toPlainString()
                            is StopMarket -> executionType.trigger.toPlainString()
                            is TrailingStop -> executionType.trailingStop?.toPlainString() ?: ""
                        },
                        timestamp = openOrder
                            .createdAt
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                            .format(TradeDateTimeFormat),
                    )
                }
            }
        }.collectAsState(emptyList())
    }

    private fun onAdvanceReplay() {
        barReplay.advance()
    }

    private fun onAdvanceReplayByBar() {
        barReplay.advanceToClose()
    }

    private fun onSetIsAutoNextEnabled(isAutoNextEnabled: Boolean) {

        autoNextJob = when {
            // Don't create new job if one already exists
            isAutoNextEnabled -> when {
                autoNextJob == null -> {
                    coroutineScope.launch {
                        while (isActive) {
                            delay(1.seconds)
                            barReplay.advance()
                        }
                    }
                }

                else -> autoNextJob
            }

            else -> {
                autoNextJob?.cancel()
                null
            }
        }
    }

    private fun onBuy(stockChart: StockChart) = coroutineScope.launchUnit {

        val price = stockChart.data
            .candleSeries
            .last()
            .close
            .toPlainString()

        val params = OrderFormParams(
            id = Uuid.random(),
            stockChartParams = stockChart.params,
            initialModel = ReplayOrderFormModel(
                isBuy = true,
                price = price,
            ),
        )

        orderFormWindowsManager.newWindow(params)
    }

    private fun onSell(stockChart: StockChart) = coroutineScope.launchUnit {

        val price = stockChart.data
            .candleSeries
            .last()
            .close
            .toPlainString()

        val params = OrderFormParams(
            id = Uuid.random(),
            stockChartParams = stockChart.params,
            initialModel = ReplayOrderFormModel(
                isBuy = false,
                price = price,
            ),
        )

        orderFormWindowsManager.newWindow(params)
    }

    private fun onCancelOrder(id: BacktestOrderId) {
        replayOrdersManager.cancelOrder(id)
    }

    private fun getChartInfo(stockChart: StockChart) = ReplayChartInfo(
        replayTime = flow {

            val tz = TimeZone.currentSystemDefault()

            stockChart.data
                .source
                .let { it as ReplayCandleSource }
                .replaySeries
                .await()
                .replayTime
                .map { it.toLocalDateTime(tz).format(ReplayDateTimeFormat) }
                .emitInto(this)
        },
        candleState = flow {

            stockChart.data
                .source
                .let { it as ReplayCandleSource }
                .replaySeries
                .await()
                .candleState
                .map { it.name }
                .emitInto(this)
        },
    )

    private companion object {

        private val ReplayDateTimeFormat = LocalDateTime.Format {
            monthName(MonthNames.ENGLISH_ABBREVIATED)
            char(' ')
            day(padding = Padding.NONE)
            chars(", ")
            year()
            char(' ')
            hour()
            char(':')
            minute()
            char(':')
            second()
        }
    }
}
