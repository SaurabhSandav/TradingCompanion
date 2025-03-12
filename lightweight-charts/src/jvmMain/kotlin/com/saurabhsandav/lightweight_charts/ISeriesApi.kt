package com.saurabhsandav.lightweight_charts

import com.saurabhsandav.lightweight_charts.data.BarsInfo
import com.saurabhsandav.lightweight_charts.data.LogicalRange
import com.saurabhsandav.lightweight_charts.data.SeriesData
import com.saurabhsandav.lightweight_charts.options.PriceLineOptions
import com.saurabhsandav.lightweight_charts.options.SeriesOptions
import com.saurabhsandav.lightweight_charts.options.SeriesOptionsCommon
import com.saurabhsandav.lightweight_charts.utils.LwcJson
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement

class ISeriesApi<D : SeriesData, O : SeriesOptions>(
    private val definition: SeriesDefinition<D, O>,
    private val executeJs: (String) -> Unit,
    private val executeJsWithResult: suspend (String) -> String,
    val id: String,
    seriesInstanceReference: String,
) {

    private val paneReference = "$seriesInstanceReference.pane"
    private val priceLineMapReference = "$seriesInstanceReference.priceLinesMap"
    private val primitivesMapReference = "$seriesInstanceReference.primitivesMap"

    val reference = "$seriesInstanceReference.series"
    val priceScale: IPriceScaleApi = IPriceScaleApi(reference, executeJs)

    private var nextPriceLineId = 0
    private var nextPrimitiveId = 0

    private val _pane by lazy { IPaneApi(executeJs, executeJsWithResult, paneReference) }
    private val primitivesMap = mutableMapOf<ISeriesPrimitive, Int>()

    suspend fun barsInLogicalRange(range: LogicalRange?): BarsInfo? {

        val rangeJson = range?.let(LwcJson::encodeToString) ?: ""

        val result = executeJsWithResult("$reference.barsInLogicalRange($rangeJson)")

        return LwcJson.decodeFromString(result)
    }

    fun applyOptions(options: SeriesOptionsCommon) {

        val optionsJson = LwcJson.encodeToJsonElement(options)

        executeJs("$reference.applyOptions($optionsJson)")
    }

    fun applyOptions(options: O) {

        val optionsJson = LwcJson.encodeToJsonElement(definition.optionsSerializer, options)

        executeJs("$reference.applyOptions($optionsJson)")
    }

    fun setData(list: List<D>) {

        val dataJson = LwcJson.encodeToString(ListSerializer(definition.dataSerializer), list)

        executeJs("$reference.setData($dataJson);")
    }

    fun update(
        data: D,
        historicalUpdate: Boolean? = null,
    ) {

        val arguments = buildString {
            append(LwcJson.encodeToString(definition.dataSerializer, data))
            historicalUpdate?.let {
                append(", ")
                append(it)
            }
        }

        executeJs("$reference.update($arguments);")
    }

    fun createPriceLine(options: PriceLineOptions): IPriceLine {

        val optionsJson = LwcJson.encodeToString(options)

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

    fun getMouseEventDataFrom(seriesData: Map<String, JsonElement>): D? {
        val json = seriesData[id] ?: return null
        return LwcJson.decodeFromJsonElement(definition.dataSerializer, json)
    }

    fun moveToPane(paneIndex: Int) {
        executeJs("$reference.moveTo($paneIndex);")
    }

    fun getPane(): IPaneApi {

        executeJs("$paneReference = $reference.getPane();")

        return _pane
    }
}
