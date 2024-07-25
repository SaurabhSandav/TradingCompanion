package com.saurabhsandav.core.chart

import com.saurabhsandav.core.chart.data.SeriesData
import com.saurabhsandav.core.chart.data.SeriesMarker
import com.saurabhsandav.core.chart.misc.BarsInfo
import com.saurabhsandav.core.chart.misc.LogicalRange
import com.saurabhsandav.core.chart.options.PriceLineOptions
import com.saurabhsandav.core.chart.options.SeriesOptions
import kotlinx.serialization.json.JsonArray

class ISeriesApi<T : SeriesData>(
    private val executeJs: (String) -> Unit,
    private val executeJsWithResult: suspend (String) -> String,
    val name: String,
    seriesInstanceReference: String,
) {

    private val priceLineMapReference = "$seriesInstanceReference.priceLinesMap"
    private val primitivesMapReference = "$seriesInstanceReference.primitivesMap"

    val reference = "$seriesInstanceReference.series"
    val priceScale: IPriceScaleApi = IPriceScaleApi(reference, executeJs)

    private var nextPriceLineId = 0
    private var nextPrimitiveId = 0

    private val primitivesMap = mutableMapOf<ISeriesPrimitive, Int>()

    suspend fun barsInLogicalRange(range: LogicalRange): BarsInfo? {

        val result = executeJsWithResult("$reference.barsInLogicalRange(${range.toJsonElement()})")

        return BarsInfo.fromJson(result)
    }

    fun applyOptions(options: SeriesOptions) {

        val optionsJson = options.toJsonElement()

        executeJs("$reference.applyOptions(${optionsJson})")
    }

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

    fun attachPrimitive(primitive: ISeriesPrimitive) {

        val id = nextPrimitiveId++
        primitivesMap[primitive] = id

        val ref = "$primitivesMapReference.get($id)"
        val initializerStr = primitive.initializer { executeJs("$ref.$it") }

        executeJs("$primitivesMapReference.set($id, $initializerStr);")

        executeJs("$reference.attachPrimitive($ref);")
    }

    fun detachPrimitive(primitive: ISeriesPrimitive) {

        val id = primitivesMap.remove(primitive) ?: return

        executeJs("$reference.detachPrimitive($primitivesMapReference.get($id));")

        executeJs("$primitivesMapReference.delete($id);")
    }
}
