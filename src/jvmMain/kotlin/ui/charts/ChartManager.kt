package ui.charts

import AppModule
import chart.IChartApi
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.russhwolf.settings.coroutines.FlowSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import trading.Candle
import trading.MutableCandleSeries
import trading.Timeframe
import trading.dailySessionStart
import trading.data.CandleRepository
import trading.indicator.ClosePriceIndicator
import trading.indicator.EMAIndicator
import trading.indicator.VWAPIndicator
import ui.charts.ui.Chart
import utils.PrefDefaults
import utils.PrefKeys
import kotlin.time.Duration.Companion.days

internal class ChartManager(
    val appModule: AppModule,
    val params: ChartParams,
    actualChart: IChartApi,
    private val appPrefs: FlowSettings = appModule.appPrefs,
    private val candleRepo: CandleRepository = CandleRepository(appModule),
) {

    val coroutineScope = CoroutineScope(Dispatchers.Main)
    val chart = Chart(
        actualChart = actualChart,
        coroutineScope = coroutineScope,
        onLoadMore = ::onLoadMore,
    )

    private val candleSeries = MutableCandleSeries(emptyList(), params.timeframe)
    private val ema9Indicator = EMAIndicator(ClosePriceIndicator(candleSeries), length = 9)
    private val vwapIndicator = VWAPIndicator(candleSeries, ::dailySessionStart)

    private val downloadIntervalDays = 90.days

    init {

        // Setting dark mode according to settings
        coroutineScope.launch {
            appPrefs.getBooleanFlow(PrefKeys.DarkModeEnabled, PrefDefaults.DarkModeEnabled).collect { isDark ->
                chart.setDarkMode(isDark)
            }
        }

        // Initial data
        coroutineScope.launch {

            val initialCandles = getCandles(
                symbol = params.symbol,
                timeframe = params.timeframe,
                // Range of 3 months before current time to current time
                range = run {
                    val currentTime = Clock.System.now()
                    currentTime.minus(downloadIntervalDays)..currentTime
                }
            )

            initialCandles.forEach(candleSeries::addCandle)

            setInitialData()
        }
    }

    fun withNewChart(
        id: Int,
        actualChart: IChartApi,
    ) = ChartManager(
        appModule = appModule,
        params = params.copy(id = id),
        actualChart = actualChart,
    )

    fun withNewSymbol(symbol: String) = ChartManager(
        appModule = appModule,
        params = params.copy(symbol = symbol),
        actualChart = chart.actualChart,
    )

    fun withNewTimeframe(timeframe: Timeframe) = ChartManager(
        appModule = appModule,
        params = params.copy(timeframe = timeframe),
        actualChart = chart.actualChart,
    )

    private fun setInitialData() {

        val data = candleSeries.mapIndexed { index, candle ->
            Chart.Data(
                candle = candle,
                ema9 = ema9Indicator[index],
                vwap = vwapIndicator[index],
            )
        }

        chart.setData(data, hasVolume = params.symbol != "NIFTY50")
    }

    private suspend fun onLoadMore() {

        val firstCandleInstant = candleSeries.first().openInstant

        val candles = getCandles(
            params.symbol,
            params.timeframe,
            firstCandleInstant.minus(downloadIntervalDays)..firstCandleInstant
        )

        if (candles.isNotEmpty()) {
            candleSeries.prependCandles(candles)
            setInitialData()
        }
    }

    private suspend fun getCandles(
        symbol: String,
        timeframe: Timeframe,
        range: ClosedRange<Instant>,
    ): List<Candle> {

        val candlesResult = candleRepo.getCandles(
            symbol = symbol,
            timeframe = timeframe,
            from = range.start,
            to = range.endInclusive,
        )

        return when (candlesResult) {
            is Ok -> candlesResult.value
            is Err -> when (val error = candlesResult.error) {
                is CandleRepository.Error.AuthError -> error("AuthError")
                is CandleRepository.Error.UnknownError -> error(error.message)
            }
        }
    }

    data class ChartParams(
        val id: Int,
        val symbol: String,
        val timeframe: Timeframe,
    )
}
