package chart.histogram

import chart.ISeriesApi

internal class HistogramSeries(
    private val executeJs: (String) -> Unit,
    override val name: String,
) : ISeriesApi<HistogramData> {

    override fun setData(list: List<HistogramData>) {

        val dataJson = list.toJson()

        executeJs("$name.setData($dataJson);")
    }

    private fun List<HistogramData>.toJson(): String = buildString {

        append("[")

        this@toJson.forEach {
            append("{ time: ${it.time}, value: ${it.value} },")
        }

        append("]")
    }
}
