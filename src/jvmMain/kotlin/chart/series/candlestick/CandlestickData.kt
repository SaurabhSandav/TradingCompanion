package chart.series.candlestick

import chart.series.ChartData
import chart.series.Time
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class CandlestickData(
    val time: Time,
    val open: Number,
    val high: Number,
    val low: Number,
    val close: Number,
) : ChartData {

    fun toJsonObject(): JsonObject = buildJsonObject {
        put("time", time.toJsonElement())
        put("open", open)
        put("high", high)
        put("low", low)
        put("close", close)
    }
}
