package chart.data

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class CandlestickData(
    val time: Time,
    val open: Number,
    val high: Number,
    val low: Number,
    val close: Number,
) : SeriesData {

    override fun toJsonElement(): JsonElement = buildJsonObject {
        put("time", time.toJsonElement())
        put("open", open)
        put("high", high)
        put("low", low)
        put("close", close)
    }
}
