package chart

import chart.data.SeriesData
import chart.data.SeriesMarker
import chart.options.PriceLineOptions
import kotlinx.serialization.json.JsonArray

class ISeriesApi<T : SeriesData>(
    private val executeJs: (String) -> Unit,
    val name: String,
    seriesInstanceReference: String,
) {

    private val priceLineMapReference = "$seriesInstanceReference.priceLinesMap"

    val reference = "$seriesInstanceReference.series"
    val priceScale: IPriceScaleApi = IPriceScaleApi(reference, executeJs)

    private var nextPriceLineId = 0

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

    fun createPriceLine(options: PriceLineOptions): IPriceLine {

        val optionsJson = options.toJsonElement()

        val id = nextPriceLineId++

        executeJs("$priceLineMapReference.set($id, $reference.createPriceLine($optionsJson));")

        return IPriceLine(
            executeJs = executeJs,
            id = id,
            reference = "$priceLineMapReference.get($id)",
        )
    }

    fun removePriceLine(line: IPriceLine) {

        executeJs("$reference.removePriceLine(${line.reference});")

        executeJs("$priceLineMapReference.delete(${line.id});")
    }
}
