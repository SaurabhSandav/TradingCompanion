package chart.callbacks

import chart.ISeriesApi
import chart.data.CandlestickData
import chart.data.GeneralData
import chart.data.Time

fun interface MouseEventHandler {

    fun onEvent(params: MouseEventParams)
}

data class MouseEventParams(
    val time: Time?,
    val point: Point?,
    val seriesPrices: Map<ISeriesApi<*>, GeneralData>,
) {

    fun getSeriesPrices(key: ISeriesApi<CandlestickData>): GeneralData.BarPrices? {
        return seriesPrices[key] as GeneralData.BarPrices?
    }

    fun getSeriesPrice(key: ISeriesApi<*>): GeneralData.BarPrice? {
        return seriesPrices[key] as GeneralData.BarPrice?
    }
}

data class Point(
    val x: Float,
    val y: Float,
)
