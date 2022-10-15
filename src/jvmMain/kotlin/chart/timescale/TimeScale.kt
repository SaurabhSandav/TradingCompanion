package chart.timescale

import chart.IChartApi

internal class TimeScale(
    private val chart: IChartApi,
    private val executeJs: (String) -> Unit,
) {

    fun fitContent() {
        executeJs("${chart.name}.timeScale().fitContent();")
    }

    fun applyOptions(options: TimeScaleOptions) {
        executeJs("${chart.name}.timeScale().applyOptions(${options.toJson()});")
    }
}
