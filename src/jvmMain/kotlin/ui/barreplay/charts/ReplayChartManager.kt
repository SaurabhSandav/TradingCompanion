package ui.barreplay.charts

import AppModule
import com.russhwolf.settings.coroutines.FlowSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import trading.Candle
import trading.CandleSeries
import trading.Timeframe
import trading.barreplay.BarReplaySession
import trading.dailySessionStart
import trading.indicator.ClosePriceIndicator
import trading.indicator.EMAIndicator
import trading.indicator.VWAPIndicator
import ui.barreplay.charts.ui.ReplayChart
import utils.PrefDefaults
import utils.PrefKeys

internal class ReplayChartManager(
    initialParams: ChartParams,
    container: String,
    name: String,
    private val appModule: AppModule,
    appPrefs: FlowSettings = appModule.appPrefs,
) {

    val coroutineScope = CoroutineScope(Dispatchers.Main)
    val chart = ReplayChart(
        container = container,
        name = name,
        )
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
        id: Int,
        container: String,
        name: String,
        replaySession: BarReplaySession,
    ) = ReplayChartManager(
        initialParams = params.copy(id = id, replaySession = replaySession),
        container = container,
        name = name,
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
        val id: Int,
        val symbol: String,
        val timeframe: Timeframe,
        val replaySession: BarReplaySession,
    )

    private class ChartData(val replaySeries: CandleSeries) {

        val ema9Indicator = EMAIndicator(ClosePriceIndicator(replaySeries), length = 9)
        val vwapIndicator = VWAPIndicator(replaySeries, ::dailySessionStart)
    }
}
