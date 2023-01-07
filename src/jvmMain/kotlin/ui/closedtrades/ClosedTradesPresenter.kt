package ui.closedtrades

import AppModule
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import chart.data.*
import chart.options.PriceLineOptions
import chart.options.common.LineStyle
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.AppDB
import com.saurabhsandav.core.GetAllClosedTradesDetailed
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import fyers_api.FyersApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import launchUnit
import model.Side
import trading.MutableCandleSeries
import trading.Timeframe
import trading.asCandleSeries
import trading.dailySessionStart
import trading.data.CandleRepository
import trading.indicator.ClosePriceIndicator
import trading.indicator.EMAIndicator
import trading.indicator.VWAPIndicator
import ui.closedtrades.model.*
import ui.closedtrades.model.ClosedTradesEvent.DeleteTrade
import ui.closedtrades.model.ClosedTradesState.FyersLoginWindow
import ui.closetradeform.CloseTradeFormWindowParams
import ui.common.*
import ui.fyerslogin.FyersLoginState
import ui.pnlcalculator.PNLCalculatorWindowParams
import utils.brokerage
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import ui.closedtrades.model.ClosedTradesEvent.DeleteConfirmationDialog as DeleteConfirmationDialogEvent
import ui.closedtrades.model.ClosedTradesState.DeleteConfirmationDialog as DeleteConfirmationDialogState

internal class ClosedTradesPresenter(
    private val coroutineScope: CoroutineScope,
    private val appModule: AppModule,
    private val appPrefs: FlowSettings = appModule.appPrefs,
    private val appDB: AppDB = appModule.appDB,
    private val fyersApi: FyersApi = appModule.fyersApiFactory(),
) {

    private val events = MutableSharedFlow<ClosedTradesEvent>(extraBufferCapacity = Int.MAX_VALUE)

    private val editTradeFormWindowParams = mutableStateMapOf<UUID, CloseTradeFormWindowParams>()
    private val pnlCalculatorWindowParams = mutableStateMapOf<UUID, PNLCalculatorWindowParams>()
    private val chartWindowsManager = MultipleWindowManager<ClosedTradeChartWindowParams>()

    private var fyersLoginWindowState by mutableStateOf<FyersLoginWindow>(FyersLoginWindow.Closed)

    val state = coroutineScope.launchMolecule(RecompositionClock.ContextClock) {

        CollectEffect(events) { event ->

            when (event) {
                is ClosedTradesEvent.OpenChart -> onOpenChart(event.id)
                is ClosedTradesEvent.EditTrade -> onEditTrade(event.id)
                is ClosedTradesEvent.OpenPNLCalculator -> onOpenPNLCalculator(event.id)
                else -> Unit
            }
        }

        return@launchMolecule ClosedTradesState(
            closedTradesItems = getClosedTradeListEntries().value,
            deleteConfirmationDialogState = deleteConfirmationDialogState(events),
            editTradeFormWindowParams = editTradeFormWindowParams.values,
            pnlCalculatorWindowParams = pnlCalculatorWindowParams.values,
            chartWindowsManager = chartWindowsManager,
            fyersLoginWindowState = fyersLoginWindowState,
        )
    }

    val errors = mutableStateListOf<UIErrorMessage>()

    fun event(event: ClosedTradesEvent) {
        events.tryEmit(event)
    }

    @Composable
    private fun getClosedTradeListEntries(): State<Map<ClosedTradeListItem.DayHeader, List<ClosedTradeListItem.Entry>>> {
        return remember {
            appDB.closedTradeQueries
                .getAllClosedTradesDetailed()
                .asFlow()
                .mapToList(Dispatchers.IO)
                .map { getAllClosedTradesDetailed ->

                    getAllClosedTradesDetailed
                        .groupBy { LocalDateTime.parse(it.entryDate).date }
                        .mapKeys { (date, _) -> date.toClosedTradeListDayHeader() }
                        .mapValues { (_, list) -> list.map { it.toClosedTradeListEntry() } }
                }
        }.collectAsState(emptyMap())
    }

    private fun LocalDate.toClosedTradeListDayHeader(): ClosedTradeListItem.DayHeader {
        val formatted = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(toJavaLocalDate())
        return ClosedTradeListItem.DayHeader(formatted)
    }

    private fun GetAllClosedTradesDetailed.toClosedTradeListEntry(): ClosedTradeListItem.Entry {

        val instrumentCapitalized = instrument
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val entryBD = entry.toBigDecimal()
        val stopBD = stop?.toBigDecimalOrNull()
        val exitBD = exit.toBigDecimal()
        val quantityBD = quantity.toBigDecimal()
        val side = Side.fromString(side)

        val brokerage = brokerage(
            broker = broker,
            instrument = instrument,
            entry = entryBD,
            exit = exitBD,
            quantity = quantityBD,
            side = side,
        )

        val pnlBD = brokerage.pnl
        val netPnlBD = brokerage.netPNL

        val rValue = when (stopBD) {
            null -> null
            else -> when (side) {
                Side.Long -> pnlBD / ((entryBD - stopBD) * quantityBD)
                Side.Short -> pnlBD / ((stopBD - entryBD) * quantityBD)
            }.setScale(1, RoundingMode.HALF_EVEN).toPlainString()
        }

        val timeZone = TimeZone.of("Asia/Kolkata")
        val entryDateTime = LocalDateTime.parse(entryDate)
        val entryInstant = entryDateTime.toInstant(timeZone)
        val exitDateTime = LocalDateTime.parse(exitDate)
        val exitInstant = exitDateTime.toInstant(timeZone)
        val s = (exitInstant - entryInstant).inWholeSeconds

        val duration = "%02d:%02d:%02d".format(s / 3600, (s % 3600) / 60, (s % 60))

        return ClosedTradeListItem.Entry(
            id = id,
            broker = "$broker ($instrumentCapitalized)",
            ticker = ticker,
            quantity = lots?.let { "$quantity ($it ${if (it == 1) "lot" else "lots"})" } ?: quantity,
            side = this@toClosedTradeListEntry.side.uppercase(),
            entry = entry,
            stop = stop ?: "NA",
            duration = "${entryDateTime.time} ->\n${exitDateTime.time}\n($duration)",
            target = target ?: "NA",
            exit = exit,
            pnl = pnlBD.toPlainString() + rValue?.let { " (${it}R)" }.orEmpty(),
            isProfitable = pnlBD > BigDecimal.ZERO,
            netPnl = netPnlBD.toPlainString(),
            isNetProfitable = netPnlBD > BigDecimal.ZERO,
            fees = (pnlBD - netPnlBD).toPlainString(),
            maxFavorableExcursion = maxFavorableExcursion.orEmpty(),
            maxAdverseExcursion = maxAdverseExcursion.orEmpty(),
            persisted = persisted.toBoolean(),
            persistenceResult = persistenceResult,
        )
    }

    private fun onOpenChart(id: Long): Unit = coroutineScope.launchUnit {

        // Chart window already open
        if (chartWindowsManager.windows.any { it.params.tradeId == id }) return@launchUnit

        val closedTrade = withContext(Dispatchers.IO) {
            appDB.closedTradeQueries.getClosedTradesDetailedById(id).executeAsOne()
        }

        val entryDateTime = LocalDateTime.parse(closedTrade.entryDate)
        val exitDateTime = LocalDateTime.parse(closedTrade.exitDate)

        // Candles range of 1 month before and after trade interval
        val from = entryDateTime.date - DatePeriod(months = 1)
        val to = exitDateTime.date + DatePeriod(months = 1)
        val timeframe = Timeframe.M5

        // Get candles
        val candlesResult = CandleRepository(appModule).getCandles(
            symbol = closedTrade.ticker,
            timeframe = timeframe,
            from = from.atStartOfDayIn(TimeZone.currentSystemDefault()),
            to = to.atStartOfDayIn(TimeZone.currentSystemDefault()),
        )

        val candles = when (candlesResult) {
            is Ok -> MutableCandleSeries(candlesResult.value, timeframe).asCandleSeries()
            is Err -> when (val error = candlesResult.error) {
                is CandleRepository.Error.UnknownError -> {
                    errors += UIErrorMessage(error.message) { errors -= it }
                    return@launchUnit
                }

                is CandleRepository.Error.AuthError -> {
                    errors += UIErrorMessage(
                        message = "Please login",
                        actionLabel = "Login",
                        onActionClick = {
                            fyersLoginWindowState = FyersLoginWindow.Open(
                                FyersLoginState(
                                    fyersApi = fyersApi,
                                    appPrefs = appPrefs,
                                    onCloseRequest = { fyersLoginWindowState = FyersLoginWindow.Closed },
                                    onLoginSuccess = { onOpenChart(id) },
                                    onLoginFailure = { message ->
                                        errors += UIErrorMessage(message ?: "Unknown Error") { errors -= it }
                                    },
                                )
                            )
                        },
                        withDismissAction = true,
                        duration = UIErrorMessage.Duration.Indefinite,
                    ) { errors -= it }
                    return@launchUnit
                }
            }
        }

        // Setup indicators
        val ema9Indicator = EMAIndicator(ClosePriceIndicator(candles), length = 9)
        val vwapIndicator = VWAPIndicator(candles, ::dailySessionStart)

        val candleData = mutableListOf<CandlestickData>()
        val volumeData = mutableListOf<HistogramData>()
        val ema9Data = mutableListOf<LineData>()
        val vwapData = mutableListOf<LineData>()
        val entryInstant = entryDateTime.toInstant(TimeZone.currentSystemDefault())
        val exitInstant = exitDateTime.toInstant(TimeZone.currentSystemDefault())
        var entryIndex = 0
        var exitIndex = 0

        // Populate data
        candles.forEachIndexed { index, candle ->

            // Chart messes with timezone, work around it
            // Subtract IST Timezone difference
            val epochTime = candle.openInstant.epochSeconds
            val workaroundEpochTime = epochTime + 19800

            candleData += CandlestickData(
                time = Time.UTCTimestamp(workaroundEpochTime),
                open = candle.open,
                high = candle.high,
                low = candle.low,
                close = candle.close,
            )

            volumeData += HistogramData(
                time = Time.UTCTimestamp(workaroundEpochTime),
                value = candle.volume,
                color = when {
                    candle.close < candle.open -> Color(255, 82, 82)
                    else -> Color(0, 150, 136)
                },
            )

            ema9Data += LineData(
                time = Time.UTCTimestamp(workaroundEpochTime),
                value = ema9Indicator[index],
            )

            vwapData += LineData(
                time = Time.UTCTimestamp(workaroundEpochTime),
                value = vwapIndicator[index],
            )

            // Find entry candle index
            if (entryInstant > candle.openInstant)
                entryIndex = index

            // Find exit candle index
            if (exitInstant > candle.openInstant)
                exitIndex = index
        }

        val side = Side.fromString(closedTrade.side)

        val markers = listOf(
            SeriesMarker(
                time = Time.UTCTimestamp(candles[entryIndex].openInstant.epochSeconds + 19800),
                position = when (side) {
                    Side.Long -> SeriesMarkerPosition.BelowBar
                    Side.Short -> SeriesMarkerPosition.AboveBar
                },
                shape = when (side) {
                    Side.Long -> SeriesMarkerShape.ArrowUp
                    Side.Short -> SeriesMarkerShape.ArrowDown
                },
                color = when (side) {
                    Side.Long -> Color.Green
                    Side.Short -> Color.Red
                },
                text = when (side) {
                    Side.Long -> "Buy @ ${closedTrade.entry}"
                    Side.Short -> "Sell @ ${closedTrade.entry}"
                },
            ),
            SeriesMarker(
                time = Time.UTCTimestamp(candles[exitIndex].openInstant.epochSeconds + 19800),
                position = when (side) {
                    Side.Long -> SeriesMarkerPosition.AboveBar
                    Side.Short -> SeriesMarkerPosition.BelowBar
                },
                shape = when (side) {
                    Side.Long -> SeriesMarkerShape.ArrowDown
                    Side.Short -> SeriesMarkerShape.ArrowUp
                },
                color = when (side) {
                    Side.Long -> Color.Red
                    Side.Short -> Color.Green
                },
                text = when (side) {
                    Side.Long -> "Sell @ ${closedTrade.exit}"
                    Side.Short -> "Buy @ ${closedTrade.exit}"
                },
            ),
        )

        val priceLines = buildList {

            val stop = closedTrade.stop
            if (stop != null)
                add(
                    PriceLineOptions(
                        price = stop.toBigDecimal(),
                        color = AppColor.LossRed,
                        lineStyle = LineStyle.Solid,
                        title = "Stop",
                    )
                )

            val target = closedTrade.target
            if (target != null)
                add(
                    PriceLineOptions(
                        price = target.toBigDecimal(),
                        color = AppColor.ProfitGreen,
                        lineStyle = LineStyle.Solid,
                        title = "Target",
                    )
                )
        }

        val params = ClosedTradeChartWindowParams(
            tradeId = closedTrade.id,
            chartData = ClosedTradeChartData(
                candleData = candleData,
                volumeData = volumeData,
                ema9Data = ema9Data,
                vwapData = vwapData,
                visibilityIndexRange = (entryIndex - 30)..(exitIndex + 30),
                markers = markers,
                priceLines = priceLines,
            ),
        )

        // Open Chart
        chartWindowsManager.openNewWindow(params)
    }

    private fun onEditTrade(id: Long) {

        // Don't allow opening duplicate windows
        val isWindowAlreadyOpen = editTradeFormWindowParams.values.any {
            it.operationType is CloseTradeFormWindowParams.OperationType.EditExistingTrade && it.operationType.id == id
        }
        if (isWindowAlreadyOpen) return

        val key = UUID.randomUUID()
        val params = CloseTradeFormWindowParams(
            operationType = CloseTradeFormWindowParams.OperationType.EditExistingTrade(id),
            onCloseRequest = { editTradeFormWindowParams.remove(key) }
        )

        editTradeFormWindowParams[key] = params
    }

    private fun onOpenPNLCalculator(id: Long) {

        // Don't allow opening duplicate windows
        val isWindowAlreadyOpen = pnlCalculatorWindowParams.values.any {
            it.operationType is PNLCalculatorWindowParams.OperationType.FromClosedTrade && it.operationType.id == id
        }
        if (isWindowAlreadyOpen) return

        val key = UUID.randomUUID()
        val params = PNLCalculatorWindowParams(
            operationType = PNLCalculatorWindowParams.OperationType.FromClosedTrade(id),
            onCloseRequest = { pnlCalculatorWindowParams.remove(key) }
        )

        pnlCalculatorWindowParams[key] = params
    }

    @Composable
    private fun deleteConfirmationDialogState(events: Flow<ClosedTradesEvent>): DeleteConfirmationDialogState {

        var state by state<DeleteConfirmationDialogState> { DeleteConfirmationDialogState.Dismissed }

        CollectEffect(events) { event ->

            state = when (event) {
                is DeleteTrade -> DeleteConfirmationDialogState.Open(event.id)

                is DeleteConfirmationDialogEvent.Confirm -> {
                    deleteTrade(event.id)
                    DeleteConfirmationDialogState.Dismissed
                }

                DeleteConfirmationDialogEvent.Dismiss -> DeleteConfirmationDialogState.Dismissed
                else -> state
            }
        }

        return state
    }

    private fun deleteTrade(id: Long) = coroutineScope.launchUnit {

        withContext(Dispatchers.IO) {
            appDB.closedTradeQueries.delete(id)
        }
    }
}
