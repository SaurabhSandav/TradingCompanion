package chart

import chart.data.SeriesData
import chart.data.SeriesMarker
import kotlinx.serialization.json.JsonArray

class ISeriesApi<T : SeriesData>(
    private val executeJs: (String) -> Unit,
    val name: String,
) {

    val priceScale: IPriceScaleApi = IPriceScaleApi(name, executeJs)

    fun setData(list: List<T>) {

        val dataJson = JsonArray(list.map { it.toJsonElement() })

        executeJs("$name.setData($dataJson);")
    }

    fun setMarkers(list: List<SeriesMarker>) {

        val markersJson = JsonArray(list.map { it.toJsonElement() })

        executeJs("$name.setMarkers($markersJson);")
    }
}
