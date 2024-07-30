package com.saurabhsandav.lightweight_charts

import com.saurabhsandav.lightweight_charts.data.BarsInfo
import com.saurabhsandav.lightweight_charts.data.LogicalRange
import com.saurabhsandav.lightweight_charts.data.SeriesData
import com.saurabhsandav.lightweight_charts.data.SeriesMarker
import com.saurabhsandav.lightweight_charts.options.PriceLineOptions
import com.saurabhsandav.lightweight_charts.options.SeriesOptions
import com.saurabhsandav.lightweight_charts.utils.LwcJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString

class ISeriesApi<D : SeriesData>(
    private val dataSerializer: KSerializer<D>,
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

        val rangeJson = LwcJson.encodeToString(range)

        val result = executeJsWithResult("$reference.barsInLogicalRange(${rangeJson})")

        return LwcJson.decodeFromString(result)
    }

    fun applyOptions(options: SeriesOptions) {

        val optionsJson = options.toJsonElement()

        executeJs("$reference.applyOptions(${optionsJson})")
    }

    fun setData(list: List<D>) {

        val dataJson = LwcJson.encodeToString(ListSerializer(dataSerializer), list)

        executeJs("$reference.setData($dataJson);")
    }

    fun update(data: D) {

        val dataJson = LwcJson.encodeToString(dataSerializer, data)

        executeJs("$reference.update($dataJson);")
    }

    fun setMarkers(list: List<SeriesMarker>) {

        val markersJson = LwcJson.encodeToString(list)

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
