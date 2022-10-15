package chart.candlestick

import chart.ISeriesApi

internal class CandlestickSeries(
    private val executeJs: (String) -> Unit,
    override val name: String,
) : ISeriesApi<CandlestickData> {

    override fun setData(list: List<CandlestickData>) {

        val dataJson = list.toJson()

        executeJs("$name.setData($dataJson);")
    }

    private fun List<CandlestickData>.toJson(): String = buildString {

        append("[")

        this@toJson.forEach {
            append("{ time: ${it.time}, open: ${it.open}, high: ${it.high}, low: ${it.low}, close: ${it.close} },")
        }

        append("]")
    }
}
