package chart.series.baseline

import chart.series.ISeriesApiImpl
import chart.series.SingleValueData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray

class BaselineSeries(
    private val executeJs: (String) -> Unit,
    private val json: Json,
    name: String,
) : ISeriesApiImpl<SingleValueData>(executeJs, json, name) {

    override fun setData(list: List<SingleValueData>) {

        val dataStr = json.encodeToString(list.toJsonArray())

        executeJs("$name.setData($dataStr);")
    }

    private fun List<SingleValueData>.toJsonArray() = buildJsonArray {
        forEach {
            add(it.toJsonObject())
        }
    }
}
