package chart.series.line

import chart.series.ISeriesApiImpl
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonArray

class LineSeries(
    private val executeJs: (String) -> Unit,
    private val json: Json,
    name: String,
) : ISeriesApiImpl<LineData>(executeJs, json, name) {

    override fun setData(list: List<LineData>) {

        val dataStr = json.encodeToString(list.toJsonArray())

        executeJs("$name.setData($dataStr);")
    }

    private fun List<LineData>.toJsonArray(): JsonArray = buildJsonArray {
        forEach {
            add(it.toJsonObject())
        }
    }
}
