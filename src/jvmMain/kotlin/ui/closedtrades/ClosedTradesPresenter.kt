package ui.closedtrades

import AppModule
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import chart.misc.SeriesMarker
import chart.misc.SeriesMarkerPosition
import chart.misc.SeriesMarkerShape
import chart.series.candlestick.CandlestickData
import chart.series.data.Time
import chart.series.histogram.HistogramData
import chart.series.line.LineData
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.AppDB
import com.saurabhsandav.core.GetAllClosedTradesDetailed
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import fyers_api.FyersApi
import fyers_api.model.CandleResolution
import fyers_api.model.response.FyersResponse
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
import trading.indicator.ClosePriceIndicator
import trading.indicator.EMAIndicator
import trading.indicator.VWAPIndicator
import ui.addclosedtradedetailed.CloseTradeDetailedFormFields
import ui.closedtrades.model.*
import ui.closedtrades.model.ClosedTradesEvent.DeleteTrade
import ui.closedtrades.model.ClosedTradesState.CandleDataLoginWindow
import ui.common.CollectEffect
import ui.common.MultipleWindowManager
import ui.common.UIErrorMessage
import ui.common.state
import utils.CandleRepo
import utils.PrefKeys
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

    private val editTradeWindowsManager = MultipleWindowManager<CloseTradeDetailedFormFields.Model>()
    private val chartWindowsManager = MultipleWindowManager<ClosedTradeChartWindowParams>()

    private var candleDataLoginWindowState by mutableStateOf<CandleDataLoginWindow>(CandleDataLoginWindow.Dismissed)

    val state = coroutineScope.launchMolecule(RecompositionClock.ContextClock) {

        CollectEffect(events) { event ->

            when (event) {
                is ClosedTradesEvent.OpenChart -> onOpenChart(event.id)
                is ClosedTradesEvent.EditTrade -> onEditTrade(event.id)
                is ClosedTradesEvent.SaveTrade -> onSaveTrade(event.model)
                is ClosedTradesEvent.CandleDataLoggedIn -> onCandleDataLoggedIn(event.redirectUrl)
                is ClosedTradesEvent.DismissCandleDataWindow -> onDismissCandleDataLoginWindow()
                else -> Unit
            }
        }

        return@launchMolecule ClosedTradesState(
            closedTradesItems = getClosedTradeListEntries().value,
            deleteConfirmationDialogState = deleteConfirmationDialogState(events),
            editTradeWindowsManager = editTradeWindowsManager,
            chartWindowsManager = chartWindowsManager,
            candleDataLoginWindowState = candleDataLoginWindowState,
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

        val pnlBD = when (side) {
            Side.Long -> (exitBD - entryBD) * quantityBD
            Side.Short -> (entryBD - exitBD) * quantityBD
        }

        val netPnlBD = brokerage(
            broker = broker,
            instrument = instrument,
            entry = entryBD,
            exit = exitBD,
            quantity = quantityBD,
            side = side,
        )

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

    private fun onOpenChart(id: Int) = coroutineScope.launchUnit {

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

        // Get candles
        val candlesResult = CandleRepo(appModule).getCandles(
            symbol = closedTrade.ticker,
            resolution = CandleResolution.M5,
            from = from.atStartOfDayIn(TimeZone.currentSystemDefault()),
            to = to.atStartOfDayIn(TimeZone.currentSystemDefault()),
        )

        val candles = when (candlesResult) {
            is CandleRepo.CandleResult.Success -> candlesResult.candles
            is CandleRepo.CandleResult.UnknownError -> {
                errors += UIErrorMessage(candlesResult.throwable.message ?: "Unknown Error")
                candlesResult.throwable.printStackTrace()
                return@launchUnit
            }

            CandleRepo.CandleResult.AuthError -> {
                errors += UIErrorMessage(
                    message = "Please login",
                    actionLabel = "Login",
                    onActionClick = { candleDataLoginWindowState = CandleDataLoginWindow.Open(fyersApi.getLoginURL()) },
                    withDismissAction = true,
                    duration = UIErrorMessage.Duration.Indefinite,
                )
                return@launchUnit
            }
        }

        // Setup indicators
        val ema9Indicator = EMAIndicator(ClosePriceIndicator(candles), length = 9)
        val sessionStartTime = LocalTime(hour = 9, minute = 15)
        val vwapIndicator = VWAPIndicator(candles) { candle ->
            candle.openInstant.toLocalDateTime(TimeZone.currentSystemDefault()).time == sessionStartTime
        }

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
                time = Time.UTCTimestamp(entryInstant.epochSeconds + 19800),
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
                time = Time.UTCTimestamp(exitInstant.epochSeconds + 19800),
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

        val params = ClosedTradeChartWindowParams(
            tradeId = closedTrade.id,
            chartData = ClosedTradeChartData(
                candleData = candleData,
                volumeData = volumeData,
                ema9Data = ema9Data,
                vwapData = vwapData,
                visibilityIndexRange = (entryIndex - 30)..(exitIndex + 30),
                markers = markers,
            ),
        )

        // Open Chart
        chartWindowsManager.openNewWindow(params)
    }

    private fun onEditTrade(id: Int) = coroutineScope.launchUnit {

        // Edit window already open
        if (editTradeWindowsManager.windows.any { it.params.id == id }) return@launchUnit

        val closedTrade = withContext(Dispatchers.IO) {
            appDB.closedTradeQueries.getClosedTradesDetailedById(id).executeAsOne()
        }

        val model = CloseTradeDetailedFormFields.Model(
            id = closedTrade.id,
            ticker = closedTrade.ticker,
            quantity = closedTrade.quantity,
            isLong = Side.fromString(closedTrade.side) == Side.Long,
            entry = closedTrade.entry,
            stop = closedTrade.stop.orEmpty(),
            entryDateTime = LocalDateTime.parse(closedTrade.entryDate),
            target = closedTrade.target.orEmpty(),
            exit = closedTrade.exit,
            exitDateTime = LocalDateTime.parse(closedTrade.exitDate),
            maxFavorableExcursion = closedTrade.maxFavorableExcursion.orEmpty(),
            maxAdverseExcursion = closedTrade.maxAdverseExcursion.orEmpty(),
            tags = closedTrade.tags?.split(", ")?.let {
                if (it.size == 1 && it.first().isBlank()) emptyList() else it
            } ?: emptyList(),
            persisted = closedTrade.persisted.toBoolean(),
        )

        editTradeWindowsManager.openNewWindow(model)
    }

    private fun onSaveTrade(
        model: CloseTradeDetailedFormFields.Model,
    ) = coroutineScope.launchUnit {

        withContext(Dispatchers.IO) {

            val entryTime = model.entryDateTime.time
            val entryDateTime = model.entryDateTime.date.atTime(
                LocalTime(
                    hour = entryTime.hour,
                    minute = entryTime.minute,
                    second = entryTime.second,
                )
            )

            val exitTime = model.exitDateTime.time
            val exitDateTime = model.exitDateTime.date.atTime(
                LocalTime(
                    hour = exitTime.hour,
                    minute = exitTime.minute,
                    second = exitTime.second,
                )
            )

            appDB.transaction {

                appDB.closedTradeQueries.insert(
                    id = model.id,
                    broker = "Finvasia",
                    ticker = model.ticker!!,
                    instrument = "equity",
                    quantity = model.quantity,
                    lots = null,
                    side = (if (model.isLong) Side.Long else Side.Short).strValue,
                    entry = model.entry,
                    stop = model.stop,
                    entryDate = entryDateTime.toString(),
                    target = model.target,
                    exit = model.exit,
                    exitDate = exitDateTime.toString(),
                )

                appDB.closedTradeDetailQueries.insert(
                    closedTradeId = model.id,
                    maxFavorableExcursion = model.maxFavorableExcursion.ifBlank { null },
                    maxAdverseExcursion = model.maxAdverseExcursion.ifBlank { null },
                    tags = model.tags.joinToString(", "),
                    persisted = model.persisted.toString(),
                    persistenceResult = null,
                )
            }
        }
    }

    private fun onCandleDataLoggedIn(redirectUrl: String) = coroutineScope.launchUnit {

        candleDataLoginWindowState = CandleDataLoginWindow.Dismissed

        val accessToken = when (val response = fyersApi.getAccessToken(redirectUrl)) {
            is FyersResponse.Failure -> {
                errors += UIErrorMessage(response.message)
                return@launchUnit
            }

            is FyersResponse.Success -> response.result.accessToken
        }

        appPrefs.putString(PrefKeys.FyersAccessToken, accessToken)
    }

    private fun onDismissCandleDataLoginWindow() {
        candleDataLoginWindowState = CandleDataLoginWindow.Dismissed
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

    private fun deleteTrade(id: Int) = coroutineScope.launchUnit {

        withContext(Dispatchers.IO) {
            appDB.closedTradeQueries.delete(id)
        }
    }
}
