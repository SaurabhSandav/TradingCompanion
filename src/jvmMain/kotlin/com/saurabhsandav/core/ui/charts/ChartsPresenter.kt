package com.saurabhsandav.core.ui.charts

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.chart.data.SeriesMarker
import com.saurabhsandav.core.chart.data.SeriesMarkerPosition
import com.saurabhsandav.core.chart.data.SeriesMarkerShape
import com.saurabhsandav.core.chart.data.Time
import com.saurabhsandav.core.fyers_api.FyersApi
import com.saurabhsandav.core.trades.TradeOrdersRepo
import com.saurabhsandav.core.trades.TradesRepo
import com.saurabhsandav.core.trades.model.OrderType
import com.saurabhsandav.core.trading.Candle
import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.trading.data.CandleRepository
import com.saurabhsandav.core.ui.charts.model.ChartsEvent
import com.saurabhsandav.core.ui.charts.model.ChartsEvent.CandleFetchLoginCancelled
import com.saurabhsandav.core.ui.charts.model.ChartsState
import com.saurabhsandav.core.ui.charts.model.ChartsState.FyersLoginWindow
import com.saurabhsandav.core.ui.common.CollectEffect
import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.common.chart.offsetTimeForChart
import com.saurabhsandav.core.ui.fyerslogin.FyersLoginState
import com.saurabhsandav.core.ui.stockchart.StockChart
import com.saurabhsandav.core.ui.stockchart.StockChartsState
import com.saurabhsandav.core.utils.NIFTY50
import com.saurabhsandav.core.utils.mapList
import com.saurabhsandav.core.utils.retryIOResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

@Stable
internal class ChartsPresenter(
    coroutineScope: CoroutineScope,
    private val appModule: AppModule,
    private val appPrefs: FlowSettings = appModule.appPrefs,
    private val fyersApi: FyersApi = appModule.fyersApi,
    private val candleRepo: CandleRepository = CandleRepository(appModule),
    private val tradesRepo: TradesRepo = appModule.tradesRepo,
    private val tradeOrdersRepo: TradeOrdersRepo = appModule.tradeOrdersRepo,
) {

    private val events = MutableSharedFlow<ChartsEvent>(extraBufferCapacity = Int.MAX_VALUE)

    private val initialTicker = NIFTY50.first()
    private val initialTimeframe = Timeframe.M5
    private val stockCharts = mutableListOf<StockChart>()
    private val candleSources = mutableMapOf<Pair<String, Timeframe>, ChartCandleSource>()
    private val chartsState = StockChartsState(
        onNewChart = ::onNewChart,
        onCloseChart = ::onCloseChart,
        onChangeTicker = ::onChangeTicker,
        onChangeTimeframe = ::onChangeTimeframe,
        appModule = appModule,
    )
    private var fyersLoginWindowState by mutableStateOf<FyersLoginWindow>(FyersLoginWindow.Closed)
    private val errors = mutableStateListOf<UIErrorMessage>()

    val state = coroutineScope.launchMolecule(RecompositionClock.ContextClock) {

        CollectEffect(events) { event ->

            when (event) {
                CandleFetchLoginCancelled -> onCandleFetchLoginCancelled()
            }
        }

        return@launchMolecule ChartsState(
            chartsState = chartsState,
            fyersLoginWindowState = fyersLoginWindowState,
            errors = errors,
        )
    }

    fun event(event: ChartsEvent) {
        events.tryEmit(event)
    }

    private fun onNewChart(
        newStockChart: StockChart,
        prevStockChart: StockChart?,
    ) {

        // Cache StockChart
        stockCharts += newStockChart

        // Set chart params
        // If selected chartParams is null, this is the first chart. Initialize it with initial params.
        newStockChart.newParams(
            ticker = prevStockChart?.currentParams?.ticker ?: initialTicker,
            timeframe = prevStockChart?.currentParams?.timeframe ?: initialTimeframe,
        )
    }

    private fun onCloseChart(stockChart: StockChart) {

        // Remove chart session from cache
        stockCharts.remove(stockChart)

        // Destroy chart
        stockChart.destroy()

        // Remove unused ChartCandleSources from cache
        releaseUnusedCandleSources()
    }

    private fun onChangeTicker(stockChart: StockChart, ticker: String) {

        // New chart params
        stockChart.newParams(ticker = ticker)
    }

    private fun onChangeTimeframe(stockChart: StockChart, timeframe: Timeframe) {

        // New chart params
        stockChart.newParams(timeframe = timeframe)
    }

    private fun onCandleFetchLoginCancelled() {
        fyersLoginWindowState = FyersLoginWindow.Closed
    }

    private fun StockChart.newParams(
        ticker: String? = currentParams?.ticker,
        timeframe: Timeframe? = currentParams?.timeframe,
    ) {

        check(ticker != null && timeframe != null) {
            "Ticker ($ticker) and/or Timeframe ($timeframe) cannot be null"
        }

        val candleSource = candleSources.getOrPut(ticker to timeframe) {
            ChartCandleSource(ticker, timeframe, ::getCandles, ::getMarkers)
        }

        // Set ChartCandleSource on StockChart
        setCandleSource(candleSource)

        // Remove unused ChartCandleSources from cache
        releaseUnusedCandleSources()
    }

    private fun releaseUnusedCandleSources() {

        // CandleSources currently in use
        val usedCandleSources = stockCharts.mapNotNull { stockChart -> stockChart.source }

        // CandleSources not in use
        val unusedCandleSources = candleSources.filter { it.value !in usedCandleSources }

        // Remove unused CandleSource from cache
        unusedCandleSources.forEach { candleSources.remove(it.key) }
    }

    private suspend fun getCandles(
        ticker: String,
        timeframe: Timeframe,
        range: ClosedRange<Instant>,
    ): List<Candle> {

        // Suspend until logged in
        candleRepo.isLoggedIn().first { isLoggedIn ->

            if (!isLoggedIn && fyersLoginWindowState !is FyersLoginWindow.Open) {

                fyersLoginWindowState = FyersLoginWindow.Open(
                    FyersLoginState(
                        fyersApi = fyersApi,
                        appPrefs = appPrefs,
                        onCloseRequest = {
                            fyersLoginWindowState = FyersLoginWindow.Closed
                        },
                        onLoginSuccess = { },
                        onLoginFailure = { message ->
                            errors += UIErrorMessage(message ?: "Unknown Error") { errors -= it }
                        },
                    )
                )
            }

            isLoggedIn
        }

        // Retry until request successful
        val candlesResult = retryIOResult(
            initialDelay = 1000,
            maxDelay = 10000,
        ) {

            candleRepo.getCandles(
                ticker = ticker,
                timeframe = timeframe,
                from = range.start,
                to = range.endInclusive,
            )
        }

        return when (candlesResult) {
            is Ok -> candlesResult.value
            is Err -> when (val error = candlesResult.error) {
                is CandleRepository.Error.AuthError -> error(error.message ?: "AuthError")
                is CandleRepository.Error.UnknownError -> error(error.message)
            }
        }
    }

    private fun getMarkers(
        ticker: String,
        candleSeries: CandleSeries,
    ): Flow<List<SeriesMarker>> {

        val rangeLDT = candleSeries.first().openInstant.toLocalDateTime(TimeZone.currentSystemDefault())..
                candleSeries.last().openInstant.toLocalDateTime(TimeZone.currentSystemDefault())

        fun Instant.markerTime(): Time {

            val markerCandleIndex = candleSeries.indexOfLast { it.openInstant < this }
            val candleOpenInstant = candleSeries[markerCandleIndex].openInstant
            val offsetTime = candleOpenInstant.offsetTimeForChart()

            return Time.UTCTimestamp(offsetTime)
        }

        val orderMarkers = tradeOrdersRepo.getOrdersByTickerInInterval(ticker, rangeLDT).mapList { order ->

            val orderInstant = order.timestamp.toInstant(TimeZone.currentSystemDefault())

            SeriesMarker(
                time = orderInstant.markerTime(),
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
        }.flowOn(Dispatchers.IO)

        val tradeMarkers = tradesRepo.getByTickerInInterval(ticker, rangeLDT).map { trades ->
            trades.flatMap { trade ->

                val entryInstant = trade.entryTimestamp.toInstant(TimeZone.currentSystemDefault())

                buildList {

                    add(
                        SeriesMarker(
                            time = entryInstant.markerTime(),
                            position = SeriesMarkerPosition.AboveBar,
                            shape = SeriesMarkerShape.Circle,
                            color = Color.Green,
                        )
                    )

                    if (trade.isClosed) {

                        val exitInstant = trade.exitTimestamp!!.toInstant(TimeZone.currentSystemDefault())

                        add(
                            SeriesMarker(
                                time = exitInstant.markerTime(),
                                position = SeriesMarkerPosition.AboveBar,
                                shape = SeriesMarkerShape.Circle,
                                color = Color.Red,
                            )
                        )
                    }
                }
            }
        }.flowOn(Dispatchers.IO)

        return orderMarkers.combine(tradeMarkers) { orderMkrs, tradeMkrs ->
            (orderMkrs + tradeMkrs).sortedBy { (it.time as Time.UTCTimestamp).value }
        }
    }
}
