package chart.series.histogram

import chart.series.ISeriesApiImpl
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonArray

class HistogramSeries(
    private val executeJs: (String) -> Unit,
    private val json: Json,
    name: String,
) : ISeriesApiImpl<HistogramData>(executeJs, json, name) {

    override fun setData(list: List<HistogramData>) {

        val dataStr = json.encodeToString(list.toJsonArray())

        executeJs("$name.setData($dataStr);")
    }

    private fun List<HistogramData>.toJsonArray(): JsonArray = buildJsonArray {
        forEach {
            add(it.toJsonObject())
        }
    }
}
