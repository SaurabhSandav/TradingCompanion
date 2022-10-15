package chart.candlestick

import chart.ChartData

data class CandlestickData(
    val time: String,
    val open: String,
    val high: String,
    val low: String,
    val close: String,
) : ChartData
