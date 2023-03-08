package com.saurabhsandav.core.ui.charts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.saurabhsandav.core.trades.model.OrderType
import com.saurabhsandav.core.trading.Candle
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.trading.data.CandleRepository
import com.saurabhsandav.core.ui.charts.model.ChartsEvent
import com.saurabhsandav.core.ui.charts.model.ChartsEvent.CandleFetchLoginCancelled
import com.saurabhsandav.core.ui.charts.model.ChartsState
import com.saurabhsandav.core.ui.charts.model.ChartsState.FyersLoginWindow
import com.saurabhsandav.core.ui.common.CollectEffect
import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.fyerslogin.FyersLoginState
import com.saurabhsandav.core.ui.stockchart.StockChart
import com.saurabhsandav.core.ui.stockchart.StockChartsState
import com.saurabhsandav.core.utils.NIFTY50
import com.saurabhsandav.core.utils.mapList
import com.saurabhsandav.core.utils.retryIOResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

internal class ChartsPresenter(
    coroutineScope: CoroutineScope,
    private val appModule: AppModule,
    private val appPrefs: FlowSettings = appModule.appPrefs,
    private val fyersApi: FyersApi = appModule.fyersApi,
    private val candleRepo: CandleRepository = CandleRepository(appModule),
    private val tradeOrdersRepo: TradeOrdersRepo = appModule.tradeOrdersRepo,
) {

    private val events = MutableSharedFlow<ChartsEvent>(extraBufferCapacity = Int.MAX_VALUE)

    private val initialTicker = NIFTY50.first()
    private val initialTimeframe = Timeframe.M5
    private val chartSessions = mutableListOf<ChartSession>()
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

        // Create new chart session
        val chartSession = ChartSession(
            stockChart = newStockChart,
            getCandles = ::getCandles,
            getMarkers = ::getMarkers,
        )

        // Cache newly created chart session
        chartSessions += chartSession

        // Set chart params
        // If selected chartParams is null, this is the first chart. Initialize it with initial params.
        chartSession.newParams(
            ticker = prevStockChart?.currentParams?.ticker ?: initialTicker,
            timeframe = prevStockChart?.currentParams?.timeframe ?: initialTimeframe,
        )
    }

    private fun onCloseChart(stockChart: StockChart) {

        // Find chart session associated with StockChart
        val chartSession = chartSessions.find { it.stockChart == stockChart }.let(::requireNotNull)

        // Remove chart session from cache
        chartSessions.remove(chartSession)

        // Destroy chart
        chartSession.stockChart.destroy()
    }

    private fun onChangeTicker(stockChart: StockChart, ticker: String) {

        // Find chart session associated with StockChart
        val chartSession = chartSessions.find { it.stockChart == stockChart }.let(::requireNotNull)

        // New chart params
        chartSession.newParams(ticker = ticker)
    }

    private fun onChangeTimeframe(stockChart: StockChart, timeframe: Timeframe) {

        // Find chart session associated with StockChart
        val chartSession = chartSessions.find { it.stockChart == stockChart }.let(::requireNotNull)

        // New chart params
        chartSession.newParams(timeframe = timeframe)
    }

    private fun onCandleFetchLoginCancelled() {
        fyersLoginWindowState = FyersLoginWindow.Closed
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
        range: ClosedRange<Instant>,
    ): Flow<List<SeriesMarker>> {

        return tradeOrdersRepo.getOrdersByTickerInInterval(
            ticker = ticker,
            range = range.start.toLocalDateTime(TimeZone.currentSystemDefault())..range.endInclusive.toLocalDateTime(
                TimeZone.currentSystemDefault()
            ),
        ).mapList { order ->

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
    }
}
