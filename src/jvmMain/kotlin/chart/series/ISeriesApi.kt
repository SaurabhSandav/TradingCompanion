package chart.series

import chart.misc.SeriesMarker
import chart.series.data.ChartData
import chart.pricescale.IPriceScaleApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray

interface ISeriesApi<T : ChartData> {

    val name: String

    val priceScale: IPriceScaleApi

    fun setData(list: List<T>)

    fun setMarkers(list: List<SeriesMarker>)
}

abstract class ISeriesApiImpl<T : ChartData>(
    private val executeJs: (String) -> Unit,
    private val json: Json,
    final override val name: String,
) : ISeriesApi<T> {

    final override val priceScale: IPriceScaleApi = IPriceScaleApi(name, executeJs, json)

    final override fun setMarkers(list: List<SeriesMarker>) {

        val markersStr = json.encodeToString(list.toJsonArray())

        executeJs("$name.setMarkers($markersStr);")
    }

    private fun List<SeriesMarker>.toJsonArray() = buildJsonArray {
        forEach {
            add(it.toJsonObject())
        }
    }
}
