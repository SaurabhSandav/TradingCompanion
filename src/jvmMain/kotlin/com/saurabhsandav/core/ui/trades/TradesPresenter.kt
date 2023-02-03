package com.saurabhsandav.core.ui.trades

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.chart.data.*
import com.saurabhsandav.core.fyers_api.FyersApi
import com.saurabhsandav.core.launchUnit
import com.saurabhsandav.core.trades.TradesRepo
import com.saurabhsandav.core.trades.model.OrderType
import com.saurabhsandav.core.trades.model.Trade
import com.saurabhsandav.core.trading.MutableCandleSeries
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.trading.asCandleSeries
import com.saurabhsandav.core.trading.dailySessionStart
import com.saurabhsandav.core.trading.data.CandleRepository
import com.saurabhsandav.core.trading.indicator.ClosePriceIndicator
import com.saurabhsandav.core.trading.indicator.EMAIndicator
import com.saurabhsandav.core.trading.indicator.VWAPIndicator
import com.saurabhsandav.core.ui.common.CollectEffect
import com.saurabhsandav.core.ui.common.MultipleWindowManager
import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.fyerslogin.FyersLoginState
import com.saurabhsandav.core.ui.trades.model.*
import com.saurabhsandav.core.ui.trades.model.TradesEvent.OpenChart
import com.saurabhsandav.core.ui.trades.model.TradesState.FyersLoginWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import java.math.BigDecimal
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

internal class TradesPresenter(
    private val coroutineScope: CoroutineScope,
    private val appModule: AppModule,
    private val appPrefs: FlowSettings = appModule.appPrefs,
    private val tradesRepo: TradesRepo = TradesRepo(appModule),
    private val fyersApi: FyersApi = appModule.fyersApi,
) {

    private val events = MutableSharedFlow<TradesEvent>(extraBufferCapacity = Int.MAX_VALUE)

    private val chartWindowsManager = MultipleWindowManager<TradeChartWindowParams>()
    private var fyersLoginWindowState by mutableStateOf<FyersLoginWindow>(FyersLoginWindow.Closed)

    val state = coroutineScope.launchMolecule(RecompositionClock.ContextClock) {

        CollectEffect(events) { event ->

            when (event) {
                is OpenChart -> onOpenChart(event.id)
            }
        }

        return@launchMolecule TradesState(
            tradesItems = getTradeListEntries().value,
            chartWindowsManager = chartWindowsManager,
            fyersLoginWindowState = fyersLoginWindowState,
        )
    }

    val errors = mutableStateListOf<UIErrorMessage>()

    fun event(event: TradesEvent) {
        events.tryEmit(event)
    }

    @Composable
    private fun getTradeListEntries(): State<Map<TradeListItem.DayHeader, List<TradeListItem.Entry>>> {
        return remember {
            tradesRepo.allTrades.map { trades ->
                trades
                    .groupBy { it.entryTimestamp.date }
                    .mapKeys { (date, _) -> date.toTradeListDayHeader() }
                    .mapValues { (_, list) -> list.map { it.toTradeListEntry() } }
            }
        }.collectAsState(emptyMap())
    }

    private fun LocalDate.toTradeListDayHeader(): TradeListItem.DayHeader {
        val formatted = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(toJavaLocalDate())
        return TradeListItem.DayHeader(formatted)
    }

    private fun Trade.toTradeListEntry(): TradeListItem.Entry {

        val instrumentCapitalized = instrument
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

        val timeZone = TimeZone.of("Asia/Kolkata")
        val entryInstant = entryTimestamp.toInstant(timeZone)
        val exitInstant = exitTimestamp?.toInstant(timeZone)
        val s = exitInstant?.let { (it - entryInstant).inWholeSeconds }

        val duration = s?.let { "%02d:%02d:%02d".format(it / 3600, (it % 3600) / 60, (it % 60)) }

        return TradeListItem.Entry(
            id = id,
            broker = "$broker ($instrumentCapitalized)",
            ticker = ticker,
            side = side.toString().uppercase(),
            quantity = (lots?.let { "$closedQuantity / $quantity ($it ${if (it == 1) "lot" else "lots"})" }
                ?: "$closedQuantity / $quantity").toString(),
            entry = averageEntry.toPlainString(),
            exit = averageExit?.toPlainString() ?: "",
            duration = "${entryTimestamp.time} -> ${exitTimestamp?.time ?: "Now"}\n${duration?.let { "($it)" }}",
            pnl = pnl.toPlainString(),
            isProfitable = pnl > BigDecimal.ZERO,
            netPnl = netPnl.toPlainString(),
            isNetProfitable = netPnl > BigDecimal.ZERO,
            fees = fees.toPlainString(),
        )
    }

    private fun onOpenChart(id: Long): Unit = coroutineScope.launchUnit {

        // Chart window already open
        if (chartWindowsManager.windows.any { it.params.tradeId == id }) return@launchUnit

        val (trade, tradeOrders) = tradesRepo.getById(id).first() to tradesRepo.getOrdersForTrade(id).first()

        val exitDateTime = trade.exitTimestamp ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

        // Candles range of 1 month before and after trade interval
        val from = trade.entryTimestamp.date - DatePeriod(months = 1)
        val to = exitDateTime.date + DatePeriod(months = 1)
        val timeframe = Timeframe.M5

        // Get candles
        val candlesResult = CandleRepository(appModule).getCandles(
            symbol = trade.ticker,
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
        val entryInstant = trade.entryTimestamp.toInstant(TimeZone.currentSystemDefault())
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

        val markers = tradeOrders.map { order ->

            val orderInstant = order.timestamp.toInstant(TimeZone.currentSystemDefault())

            SeriesMarker(
                time = Time.UTCTimestamp(orderInstant.epochSeconds + 19800),
                position = when (order.type) {
                    OrderType.Buy -> SeriesMarkerPosition.BelowBar
                    OrderType.Sell -> SeriesMarkerPosition.AboveBar
                },
                shape = when (order.type) {
                    OrderType.Buy -> SeriesMarkerShape.ArrowUp
                    OrderType.Sell -> SeriesMarkerShape.ArrowDown
                },
                color = when (order.type) {
                    OrderType.Buy -> Color.Green
                    OrderType.Sell -> Color.Red
                },
                text = when (order.type) {
                    OrderType.Buy -> order.price.toPlainString()
                    OrderType.Sell -> order.price.toPlainString()
                },
            )
        }

        val params = TradeChartWindowParams(
            tradeId = trade.id,
            chartData = TradeChartData(
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
}
