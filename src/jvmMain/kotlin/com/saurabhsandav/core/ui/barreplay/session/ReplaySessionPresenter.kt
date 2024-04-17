package com.saurabhsandav.core.ui.barreplay.session

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trading.backtest.*
import com.saurabhsandav.core.trading.barreplay.BarReplay
import com.saurabhsandav.core.ui.barreplay.model.BarReplayState.ReplayParams
import com.saurabhsandav.core.ui.barreplay.session.model.ReplaySessionEvent
import com.saurabhsandav.core.ui.barreplay.session.model.ReplaySessionEvent.*
import com.saurabhsandav.core.ui.barreplay.session.model.ReplaySessionState
import com.saurabhsandav.core.ui.barreplay.session.model.ReplaySessionState.*
import com.saurabhsandav.core.ui.barreplay.session.replayorderform.model.ReplayOrderFormModel
import com.saurabhsandav.core.ui.common.TradeDateTimeFormatter
import com.saurabhsandav.core.ui.common.app.AppWindowsManager
import com.saurabhsandav.core.ui.stockchart.LoadConfig
import com.saurabhsandav.core.ui.stockchart.StockChart
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.core.utils.PrefKeys
import com.saurabhsandav.core.utils.emitInto
import com.saurabhsandav.core.utils.format
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.time.Duration.Companion.seconds

internal class ReplaySessionPresenter(
    private val coroutineScope: CoroutineScope,
    replayParams: ReplayParams,
    stockChartsStateFactory: StockChartsStateFactory,
    private val barReplay: BarReplay,
    val replayOrdersManager: ReplayOrdersManager,
    private val appPrefs: FlowSettings,
) {

    private var autoNextJob by mutableStateOf<Job?>(null)
    private val chartsState = stockChartsStateFactory(
        initialParams = StockChartParams(replayParams.initialTicker, replayParams.baseTimeframe),
        loadConfig = LoadConfig(initialLoadBefore = replayParams.replayFrom),
    )
    private val orderFormWindowsManager = AppWindowsManager<OrderFormParams>()

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule ReplaySessionState(
            chartsState = chartsState,
            selectedProfileId = getSelectedProfileId(),
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
            is ProfileSelected -> onProfileSelected(event.id)
            is Buy -> onBuy(event.stockChart)
            is Sell -> onSell(event.stockChart)
            is CancelOrder -> onCancelOrder(event.id)
        }
    }

    @Composable
    private fun getSelectedProfileId(): ProfileId? {
        return remember {
            appPrefs.getLongOrNullFlow(PrefKeys.ReplayTradingProfile).map { it?.let(::ProfileId) }
        }.collectAsState(null).value
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
                                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                            "${params.broker} ($instrumentCapitalized)"
                        },
                        ticker = params.ticker,
                        quantity = params.lots
                            ?.let { "$quantity ($it ${if (it == 1) "lot" else "lots"})" } ?: quantity.toString(),
                        side = params.side.strValue.uppercase(),
                        price = when (openOrder.executionType) {
                            is Limit -> openOrder.executionType.price.toPlainString()
                            is Market -> ""
                            is StopLimit -> openOrder.executionType.price.toPlainString()
                            is StopMarket -> openOrder.executionType.trigger.toPlainString()
                            is TrailingStop -> openOrder.executionType.trailingStop?.toPlainString() ?: ""
                        },
                        timestamp = TradeDateTimeFormatter.format(
                            ldt = openOrder.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()),
                        ),
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

    private fun onProfileSelected(id: ProfileId?) = coroutineScope.launchUnit {

        val currentProfileId = appPrefs.getLongOrNull(PrefKeys.ReplayTradingProfile)?.let(::ProfileId)

        when (id) {
            // Profile deleted, delete pref
            null -> appPrefs.remove(PrefKeys.ReplayTradingProfile)
            // Save selected profile
            else -> appPrefs.putLong(PrefKeys.ReplayTradingProfile, id.value)
        }

        if (currentProfileId != id) {

            // Close all child windows
            orderFormWindowsManager.closeAll()
        }
    }

    private fun onBuy(stockChart: StockChart) = coroutineScope.launchUnit {

        val price = stockChart.data
            .candleSeries
            .last()
            .close
            .toPlainString()

        val params = OrderFormParams(
            id = UUID.randomUUID(),
            stockChartParams = stockChart.params,
            initialModel = ReplayOrderFormModel.Initial(
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
            id = UUID.randomUUID(),
            stockChartParams = stockChart.params,
            initialModel = ReplayOrderFormModel.Initial(
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

            stockChart.data
                .source
                .let { it as ReplayCandleSource }
                .replaySeries
                .await()
                .replayTime
                .map(::formattedReplayTime)
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

    private fun formattedReplayTime(currentInstant: Instant): String {
        val localDateTime = currentInstant.toLocalDateTime(TimeZone.currentSystemDefault())
        return DateTimeFormatter.ofPattern("MMM d, yyyy\nHH:mm:ss").format(localDateTime)
    }
}
