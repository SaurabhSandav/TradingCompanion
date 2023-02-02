package com.saurabhsandav.core.ui.barreplay.charts

import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.chart.IChartApi
import com.saurabhsandav.core.trading.Candle
import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.trading.barreplay.BarReplaySession
import com.saurabhsandav.core.trading.dailySessionStart
import com.saurabhsandav.core.trading.indicator.ClosePriceIndicator
import com.saurabhsandav.core.trading.indicator.EMAIndicator
import com.saurabhsandav.core.trading.indicator.VWAPIndicator
import com.saurabhsandav.core.ui.barreplay.charts.ui.ReplayChart
import com.saurabhsandav.core.utils.PrefDefaults
import com.saurabhsandav.core.utils.PrefKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class ReplayChartManager(
    initialParams: ChartParams,
    actualChart: IChartApi,
    private val appModule: AppModule,
    appPrefs: FlowSettings = appModule.appPrefs,
) {

    val coroutineScope = CoroutineScope(Dispatchers.Main)
    val chart = ReplayChart(actualChart)
    var params = initialParams
        private set

    private var data = ChartData(initialParams.replaySession.replaySeries)
    private var liveCandleJob: Job

    init {

        // Setting dark mode according to settings
        coroutineScope.launch {
            appPrefs.getBooleanFlow(PrefKeys.DarkModeEnabled, PrefDefaults.DarkModeEnabled).collect { isDark ->
                chart.setDarkMode(isDark)
            }
        }

        // Initial data
        setInitialData()

        // Update chart with live candles
        liveCandleJob = coroutineScope.launch {
            data.replaySeries.live.collect(::update)
        }
    }

    fun reset() {
        setInitialData()
    }

    fun withNewChart(
        tabId: Int,
        actualChart: IChartApi,
        replaySession: BarReplaySession,
    ) = ReplayChartManager(
        initialParams = params.copy(tabId = tabId, replaySession = replaySession),
        actualChart = actualChart,
        appModule = appModule,
    )

    fun changeSymbol(
        symbol: String,
        replaySession: BarReplaySession,
    ) {

        // Update params
        params = params.copy(symbol = symbol, replaySession = replaySession)

        // Set data
        data = ChartData(replaySession.replaySeries)

        // Update chart
        setInitialData()

        // Update chart with live candles
        liveCandleJob.cancel()
        liveCandleJob = coroutineScope.launch {
            replaySession.replaySeries.live.collect(::update)
        }
    }

    fun changeTimeframe(
        timeframe: Timeframe,
        replaySession: BarReplaySession,
    ) {

        // Update params
        params = params.copy(timeframe = timeframe, replaySession = replaySession)

        // Set data
        data = ChartData(replaySession.replaySeries)

        // Update chart
        setInitialData()

        // Update chart with live candles
        liveCandleJob.cancel()
        liveCandleJob = coroutineScope.launch {
            replaySession.replaySeries.live.collect(::update)
        }
    }

    private fun setInitialData() {

        val data = data.replaySeries.mapIndexed { index, candle ->
            ReplayChart.Data(
                candle = candle,
                ema9 = data.ema9Indicator[index],
                vwap = data.vwapIndicator[index],
            )
        }

        chart.setData(data, hasVolume = params.symbol != "NIFTY50")
    }

    private fun update(candle: Candle) {

        val index = data.replaySeries.indexOf(candle)

        chart.update(
            ReplayChart.Data(
                candle = candle,
                ema9 = data.ema9Indicator[index],
                vwap = data.vwapIndicator[index],
            )
        )
    }

    data class ChartParams(
        val tabId: Int,
        val symbol: String,
        val timeframe: Timeframe,
        val replaySession: BarReplaySession,
    )

    private class ChartData(val replaySeries: CandleSeries) {

        val ema9Indicator = EMAIndicator(ClosePriceIndicator(replaySeries), length = 9)
        val vwapIndicator = VWAPIndicator(replaySeries, ::dailySessionStart)
    }
}
