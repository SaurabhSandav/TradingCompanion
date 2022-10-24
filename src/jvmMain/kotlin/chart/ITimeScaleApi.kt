package chart

import chart.data.Time
import chart.options.TimeScaleOptions
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ITimeScaleApi(
    private val chart: IChartApi,
    private val executeJs: (String) -> Unit,
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

        val optionsJson = options.toJsonElement()

        executeJs("${chart.name}.timeScale().applyOptions($optionsJson);")
    }
}
