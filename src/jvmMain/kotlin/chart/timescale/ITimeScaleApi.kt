package chart.timescale

import chart.IChartApi
import chart.series.data.Time
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ITimeScaleApi(
    private val chart: IChartApi,
    private val executeJs: (String) -> Unit,
    private val json: Json,
) {

    fun scrollToPosition(position: Int, animated: Boolean) {
        executeJs("${chart.name}.timeScale().scrollToPosition($position, $animated);")
    }

    fun setVisibleRange(from: Time, to: Time) {

        val rangeStr = buildJsonObject {
            put("from", from.toJsonElement())
            put("to", to.toJsonElement())
        }

        executeJs("${chart.name}.timeScale().setVisibleRange($rangeStr);")
    }

    fun setVisibleLogicalRange(from: Int, to: Int) {

        val rangeStr = buildJsonObject {
            put("from", from)
            put("to", to)
        }

        executeJs("${chart.name}.timeScale().setVisibleLogicalRange($rangeStr);")
    }

    fun fitContent() {
        executeJs("${chart.name}.timeScale().fitContent();")
    }

    fun applyOptions(options: TimeScaleOptions) {

        val optionsStr = json.encodeToString(options.toJsonObject())

        executeJs("${chart.name}.timeScale().applyOptions($optionsStr);")
    }
}
