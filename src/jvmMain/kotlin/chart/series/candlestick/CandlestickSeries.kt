package chart.series.candlestick

import chart.series.ISeriesApiImpl
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray

class CandlestickSeries(
    private val executeJs: (String) -> Unit,
    private val json: Json,
    name: String,
) : ISeriesApiImpl<CandlestickData>(executeJs, json, name) {

    override fun setData(list: List<CandlestickData>) {

        val dataStr = json.encodeToString(list.toJsonArray())

        executeJs("$name.setData($dataStr);")
    }

    private fun List<CandlestickData>.toJsonArray() = buildJsonArray {
        forEach {
            add(it.toJsonObject())
        }
    }
}
