package chart

import chart.data.SeriesData
import chart.data.SeriesMarker
import kotlinx.serialization.json.JsonArray

class ISeriesApi<T : SeriesData>(
    private val executeJs: (String) -> Unit,
    val name: String,
    val reference: String,
) {

    val priceScale: IPriceScaleApi = IPriceScaleApi(reference, executeJs)

    fun setData(list: List<T>) {

        val dataJson = JsonArray(list.map { it.toJsonElement() })

        executeJs("$reference.setData($dataJson);")
    }

    fun update(data: T) {

        val dataJson = data.toJsonElement()

        executeJs("$reference.update($dataJson);")
    }

    fun setMarkers(list: List<SeriesMarker>) {

        val markersJson = JsonArray(list.map { it.toJsonElement() })

        executeJs("$reference.setMarkers($markersJson);")
    }
}
