package chart.callbacks

import chart.ISeriesApi
import chart.data.GeneralData
import chart.data.Time

fun interface MouseEventHandler {

    fun onEvent(params: MouseEventParams)
}

data class MouseEventParams(
    val time: Time?,
    val point: Point?,
    val seriesPrices: Map<ISeriesApi<*>, GeneralData>,
)

data class Point(
    val x: Float,
    val y: Float,
)
