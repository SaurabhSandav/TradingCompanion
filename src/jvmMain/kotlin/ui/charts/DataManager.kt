package ui.charts

import trading.CandleSeries
import trading.Timeframe
import trading.dailySessionStart
import trading.indicator.ClosePriceIndicator
import trading.indicator.EMAIndicator
import trading.indicator.VWAPIndicator
import ui.charts.ui.Chart

internal class DataManager(
    val chartId: Int,
    val symbol: String,
    val timeframe: Timeframe,
    val chart: Chart,
    val candleSeries: CandleSeries,
) {

    private val ema9Indicator = EMAIndicator(ClosePriceIndicator(candleSeries), length = 9)
    private val vwapIndicator = VWAPIndicator(candleSeries, ::dailySessionStart)

    init {
        setInitialData()
    }

    private fun setInitialData() {

        val data = candleSeries.mapIndexed { index, candle ->
            Chart.Data(
                candle = candle,
                ema9 = ema9Indicator[index],
                vwap = vwapIndicator[index],
            )
        }

        chart.setData(data, hasVolume = symbol != "NIFTY50")
    }
}
