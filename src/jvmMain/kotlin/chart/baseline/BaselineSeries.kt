package chart.baseline

import chart.ISeriesApi

internal class BaselineSeries(
    private val executeJs: (String) -> Unit,
    override val name: String,
) : ISeriesApi<BaselineData> {

    override fun setData(list: List<BaselineData>) {

        val dataJson = list.toJson()

        executeJs("$name.setData($dataJson);")
    }

    private fun List<BaselineData>.toJson(): String = buildString {

        append("[")

        this@toJson.forEach {
            append("{ time: \"${it.time}\", value: ${it.value} },")
        }

        append("]")
    }
}
