package chart.timescale

import chart.IChartApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ITimeScaleApi(
    private val chart: IChartApi,
    private val executeJs: (String) -> Unit,
    private val json: Json,
) {

    fun fitContent() {
        executeJs("${chart.name}.timeScale().fitContent();")
    }

    fun applyOptions(options: TimeScaleOptions) {

        val optionsStr = json.encodeToString(options.toJsonObject())

        executeJs("${chart.name}.timeScale().applyOptions($optionsStr);")
    }
}
