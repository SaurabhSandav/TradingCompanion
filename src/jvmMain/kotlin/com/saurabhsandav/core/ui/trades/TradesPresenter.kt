package com.saurabhsandav.core.ui.trades

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.chart.data.*
import com.saurabhsandav.core.fyers_api.FyersApi
import com.saurabhsandav.core.trades.Trade
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.TradeExecutionSide
import com.saurabhsandav.core.trading.DailySessionChecker
import com.saurabhsandav.core.trading.MutableCandleSeries
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.trading.asCandleSeries
import com.saurabhsandav.core.trading.data.CandleRepository
import com.saurabhsandav.core.trading.indicator.ClosePriceIndicator
import com.saurabhsandav.core.trading.indicator.EMAIndicator
import com.saurabhsandav.core.trading.indicator.VWAPIndicator
import com.saurabhsandav.core.ui.TradeContentLauncher
import com.saurabhsandav.core.ui.common.TradeDateTimeFormatter
import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.common.app.AppWindowsManager
import com.saurabhsandav.core.ui.common.chart.offsetTimeForChart
import com.saurabhsandav.core.ui.loginservice.LoginServicesManager
import com.saurabhsandav.core.ui.loginservice.ResultHandle
import com.saurabhsandav.core.ui.loginservice.impl.FyersLoginService
import com.saurabhsandav.core.ui.trades.model.TradeChartData
import com.saurabhsandav.core.ui.trades.model.TradeChartWindowParams
import com.saurabhsandav.core.ui.trades.model.TradesEvent
import com.saurabhsandav.core.ui.trades.model.TradesEvent.OpenChart
import com.saurabhsandav.core.ui.trades.model.TradesEvent.OpenDetails
import com.saurabhsandav.core.ui.trades.model.TradesState
import com.saurabhsandav.core.ui.trades.model.TradesState.ProfileTradeId
import com.saurabhsandav.core.ui.trades.model.TradesState.TradeEntry
import com.saurabhsandav.core.utils.*
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import java.math.BigDecimal
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Stable
internal class TradesPresenter(
    private val coroutineScope: CoroutineScope,
    private val appModule: AppModule,
    private val tradeContentLauncher: TradeContentLauncher,
    private val appPrefs: FlowSettings = appModule.appPrefs,
    private val candleRepo: CandleRepository = appModule.candleRepo,
    private val tradingProfiles: TradingProfiles = appModule.tradingProfiles,
    private val loginServicesManager: LoginServicesManager = appModule.loginServicesManager,
    private val fyersApi: FyersApi = appModule.fyersApi,
) {

    private val chartWindowsManager = AppWindowsManager<TradeChartWindowParams>()
    private val errors = mutableStateListOf<UIErrorMessage>()

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule TradesState(
            openTrades = getOpenTrades().value,
            todayTrades = getTodayTrades().value,
            pastTrades = getPastTrades().value,
            chartWindowsManager = chartWindowsManager,
            errors = remember(errors) { errors.toImmutableList() },
            eventSink = ::onEvent,
        )
    }

    private fun onEvent(event: TradesEvent) {

        when (event) {
            is OpenDetails -> onOpenDetails(event.profileTradeId)
            is OpenChart -> onOpenChart(event.profileTradeId)
        }
    }

    @Composable
    private fun getOpenTrades(): State<ImmutableList<TradeEntry>> {
        return remember {
            tradingProfiles.currentProfile.flatMapLatest { profile ->

                // Close all child windows
                chartWindowsManager.closeAll()

                val tradingRecord = tradingProfiles.getRecord(profile.id)

                tradingRecord.trades.getOpen().map { trades ->
                    trades
                        .map { it.toTradeListEntry(profile.id) }
                        .toImmutableList()
                }
            }
        }.collectAsState(persistentListOf())
    }

    @Composable
    private fun getTodayTrades(): State<ImmutableList<TradeEntry>> {
        return remember {
            tradingProfiles.currentProfile.flatMapLatest { profile ->

                val tradingRecord = tradingProfiles.getRecord(profile.id)

                tradingRecord.trades.getToday().map { trades ->
                    trades
                        .map { it.toTradeListEntry(profile.id) }
                        .toImmutableList()
                }
            }
        }.collectAsState(persistentListOf())
    }

    @Composable
    private fun getPastTrades(): State<ImmutableList<TradeEntry>> {
        return remember {
            tradingProfiles.currentProfile.flatMapLatest { profile ->

                val tradingRecord = tradingProfiles.getRecord(profile.id)

                tradingRecord.trades.getBeforeToday().map { trades ->
                    trades
                        .map { it.toTradeListEntry(profile.id) }
                        .toImmutableList()
                }
            }
        }.collectAsState(persistentListOf())
    }

    private fun Trade.toTradeListEntry(profileId: Long): TradeEntry {

        val instrumentCapitalized = instrument.strValue
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

        val timeZone = TimeZone.of("Asia/Kolkata")
        val entryInstant = entryTimestamp.toInstant(timeZone)
        val exitInstant = exitTimestamp?.toInstant(timeZone)

        fun formatDuration(duration: Duration): String {

            val durationSeconds = duration.inWholeSeconds

            return "%02d:%02d:%02d".format(
                durationSeconds / 3600,
                (durationSeconds % 3600) / 60,
                durationSeconds % 60,
            )
        }

        val durationStr = when {
            isClosed -> flowOf(formatDuration(exitInstant!! - entryInstant))
            else -> flow {
                while (true) {
                    emit(formatDuration(Clock.System.now() - entryInstant))
                    delay(1.seconds)
                }
            }
        }

        return TradeEntry(
            profileTradeId = ProfileTradeId(profileId = profileId, tradeId = id),
            broker = "$broker ($instrumentCapitalized)",
            ticker = ticker,
            side = side.toString().uppercase(),
            quantity = when {
                !isClosed -> "$closedQuantity / $quantity"
                else -> quantity.toPlainString()
            },
            entry = averageEntry.toPlainString(),
            exit = averageExit?.toPlainString() ?: "",
            entryTime = TradeDateTimeFormatter.format(entryTimestamp),
            duration = durationStr,
            pnl = pnl.toPlainString(),
            isProfitable = pnl > BigDecimal.ZERO,
            netPnl = netPnl.toPlainString(),
            isNetProfitable = netPnl > BigDecimal.ZERO,
            fees = fees.toPlainString(),
        )
    }

    private fun onOpenDetails(profileTradeId: ProfileTradeId) {

        tradeContentLauncher.openTrade(
            profileId = profileTradeId.profileId,
            tradeId = profileTradeId.tradeId,
        )
    }

    private fun onOpenChart(profileTradeId: ProfileTradeId): Unit = coroutineScope.launchUnit {

        // Chart window already open
        if (chartWindowsManager.windows.any { it.params.profileTradeId == profileTradeId }) return@launchUnit

        val tradingRecord = tradingProfiles.getRecord(profileTradeId.profileId)
        val trade = tradingRecord.trades.getById(profileTradeId.tradeId).first()
        val executions = tradingRecord.trades.getExecutionsForTrade(profileTradeId.tradeId).first()

        val exitDateTime = trade.exitTimestamp ?: Clock.System.nowIn(TimeZone.currentSystemDefault())

        // Candles range of 1 month before and after trade interval
        val from = trade.entryTimestamp.date - DatePeriod(months = 1)
        val to = exitDateTime.date + DatePeriod(months = 1)
        val timeframe = appPrefs.getStringFlow(PrefKeys.DefaultTimeframe, PrefDefaults.DefaultTimeframe.name)
            .map(Timeframe::valueOf)
            .first()

        // Get candles
        val candlesResult = candleRepo.getCandles(
            ticker = trade.ticker,
            timeframe = timeframe,
            from = from.atStartOfDayIn(TimeZone.currentSystemDefault()),
            to = to.atStartOfDayIn(TimeZone.currentSystemDefault()),
            edgeCandlesInclusive = false,
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

                            loginServicesManager.addService(
                                serviceBuilder = FyersLoginService.Builder(
                                    fyersApi = fyersApi,
                                    appPrefs = appPrefs
                                ),
                                resultHandle = ResultHandle(
                                    onFailure = { message ->
                                        errors += UIErrorMessage(message ?: "Unknown Error") { errors -= it }
                                    },
                                    onSuccess = { onOpenChart(profileTradeId) },
                                ),
                            )
                        },
                        withDismissAction = true,
                        duration = UIErrorMessage.Duration.Indefinite,
                        onNotified = { errors -= it },
                    )
                    return@launchUnit
                }
            }
        }

        // Setup indicators
        val ema9Indicator = EMAIndicator(ClosePriceIndicator(candles), length = 9)
        val vwapIndicator = VWAPIndicator(candles, DailySessionChecker)

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

            val candleTime = candle.openInstant.offsetTimeForChart()

            candleData += CandlestickData(
                time = Time.UTCTimestamp(candleTime),
                open = candle.open,
                high = candle.high,
                low = candle.low,
                close = candle.close,
            )

            volumeData += HistogramData(
                time = Time.UTCTimestamp(candleTime),
                value = candle.volume,
                color = when {
                    candle.close < candle.open -> Color(255, 82, 82)
                    else -> Color(0, 150, 136)
                },
            )

            ema9Data += LineData(
                time = Time.UTCTimestamp(candleTime),
                value = ema9Indicator[index],
            )

            vwapData += LineData(
                time = Time.UTCTimestamp(candleTime),
                value = vwapIndicator[index],
            )

            // Find entry candle index
            if (entryInstant > candle.openInstant)
                entryIndex = index

            // Find exit candle index
            if (exitInstant > candle.openInstant)
                exitIndex = index
        }

        val markers = executions.map { execution ->

            val executionInstant = execution.timestamp.toInstant(TimeZone.currentSystemDefault())

            SeriesMarker(
                time = Time.UTCTimestamp(executionInstant.offsetTimeForChart()),
                position = when (execution.side) {
                    TradeExecutionSide.Buy -> SeriesMarkerPosition.BelowBar
                    TradeExecutionSide.Sell -> SeriesMarkerPosition.AboveBar
                },
                shape = when (execution.side) {
                    TradeExecutionSide.Buy -> SeriesMarkerShape.ArrowUp
                    TradeExecutionSide.Sell -> SeriesMarkerShape.ArrowDown
                },
                color = when (execution.side) {
                    TradeExecutionSide.Buy -> Color.Green
                    TradeExecutionSide.Sell -> Color.Red
                },
                text = when (execution.side) {
                    TradeExecutionSide.Buy -> execution.price.toPlainString()
                    TradeExecutionSide.Sell -> execution.price.toPlainString()
                },
            )
        }

        val params = TradeChartWindowParams(
            profileTradeId = profileTradeId,
            chartData = TradeChartData(
                candleData = candleData.toImmutableList(),
                volumeData = volumeData.toImmutableList(),
                ema9Data = ema9Data.toImmutableList(),
                vwapData = vwapData.toImmutableList(),
                visibilityIndexRange = (entryIndex - 30)..(exitIndex + 30),
                markers = markers.toImmutableList(),
            ),
        )

        // Open Chart
        chartWindowsManager.newWindow(params)
    }
}
