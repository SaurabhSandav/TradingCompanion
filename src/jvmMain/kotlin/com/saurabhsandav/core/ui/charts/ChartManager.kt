package com.saurabhsandav.core.ui.charts

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.chart.IChartApi
import com.saurabhsandav.core.trading.Candle
import com.saurabhsandav.core.trading.MutableCandleSeries
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.trading.dailySessionStart
import com.saurabhsandav.core.trading.data.CandleRepository
import com.saurabhsandav.core.trading.indicator.ClosePriceIndicator
import com.saurabhsandav.core.trading.indicator.EMAIndicator
import com.saurabhsandav.core.trading.indicator.VWAPIndicator
import com.saurabhsandav.core.ui.charts.ui.Chart
import com.saurabhsandav.core.utils.PrefDefaults
import com.saurabhsandav.core.utils.PrefKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days

internal class ChartManager(
    val appModule: AppModule,
    initialParams: ChartParams,
    actualChart: IChartApi,
    private val onCandleDataLogin: suspend () -> Boolean,
    private val appPrefs: FlowSettings = appModule.appPrefs,
    private val candleRepo: CandleRepository = CandleRepository(appModule),
) {

    val coroutineScope = CoroutineScope(Dispatchers.Main)
    val chart = Chart(
        actualChart = actualChart,
        coroutineScope = coroutineScope,
        onLoadMore = ::onLoadMore,
    )
    var params = initialParams
    private var data = ChartData(initialParams.timeframe)

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

            initialCandles.forEach(data.candleSeries::addCandle)

            setInitialData()
        }
    }

    fun withNewChart(
        tabId: Int,
        actualChart: IChartApi,
    ) = ChartManager(
        appModule = appModule,
        initialParams = params.copy(tabId = tabId),
        actualChart = actualChart,
        onCandleDataLogin = onCandleDataLogin,
    )

    fun changeSymbol(symbol: String) {

        // Update params
        params = params.copy(symbol = symbol)

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

            data = ChartData(params.timeframe)

            initialCandles.forEach(data.candleSeries::addCandle)

            setInitialData()
        }
    }

    fun changeTimeframe(timeframe: Timeframe) {

        // Update params
        params = params.copy(timeframe = timeframe)

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

            data = ChartData(params.timeframe)

            initialCandles.forEach(data.candleSeries::addCandle)

            setInitialData()
        }
    }

    private fun setInitialData() {

        val data = data.candleSeries.mapIndexed { index, candle ->
            Chart.Data(
                candle = candle,
                ema9 = data.ema9Indicator[index],
                vwap = data.vwapIndicator[index],
            )
        }

        chart.setData(data, hasVolume = params.symbol != "NIFTY50")
    }

    private suspend fun onLoadMore() {

        val firstCandleInstant = data.candleSeries.first().openInstant

        val candles = getCandles(
            symbol = params.symbol,
            timeframe = params.timeframe,
            range = firstCandleInstant.minus(downloadIntervalDays)..firstCandleInstant
        )

        if (candles.isNotEmpty()) {
            data.candleSeries.prependCandles(candles)
            setInitialData()
        }
    }

    private suspend fun getCandles(
        symbol: String,
        timeframe: Timeframe,
        range: ClosedRange<Instant>,
        retryOnLogin: Boolean = true,
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
                is CandleRepository.Error.AuthError -> {

                    if (retryOnLogin) {
                        val loginSuccessful = onCandleDataLogin()
                        if (loginSuccessful) return getCandles(symbol, timeframe, range, false)
                    }

                    error("AuthError")
                }

                is CandleRepository.Error.UnknownError -> error(error.message)
            }
        }
    }

    data class ChartParams(
        val tabId: Int,
        val symbol: String,
        val timeframe: Timeframe,
    )

    private class ChartData(timeframe: Timeframe) {

        val candleSeries = MutableCandleSeries(emptyList(), timeframe)

        val ema9Indicator = EMAIndicator(ClosePriceIndicator(candleSeries), length = 9)
        val vwapIndicator = VWAPIndicator(candleSeries, ::dailySessionStart)
    }
}
