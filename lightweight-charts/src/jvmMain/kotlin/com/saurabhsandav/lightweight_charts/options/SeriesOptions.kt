package com.saurabhsandav.lightweight_charts.options

import com.saurabhsandav.lightweight_charts.IsJsonElement
import com.saurabhsandav.lightweight_charts.options.common.LineStyle
import com.saurabhsandav.lightweight_charts.options.common.LineWidth
import com.saurabhsandav.lightweight_charts.options.common.PriceFormat
import com.saurabhsandav.lightweight_charts.options.common.PriceLineSource
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

interface SeriesOptions : IsJsonElement

open class SeriesOptionsCommon(
    open val lastValueVisible: Boolean? = null,
    open val title: String? = null,
    open val priceScaleId: String? = null,
    open val visible: Boolean? = null,
    open val priceLineVisible: Boolean? = null,
    open val priceLineSource: PriceLineSource? = null,
    open val priceLineWidth: LineWidth? = null,
    open val priceLineColor: String? = null,
    open val priceLineStyle: LineStyle? = null,
    open val priceFormat: PriceFormat? = null,
    open val baseLineVisible: Boolean? = null,
    open val baseLineColor: String? = null,
    open val baseLineWidth: LineWidth? = null,
    open val baseLineStyle: LineStyle? = null,
) : SeriesOptions {

    protected fun JsonObjectBuilder.putSeriesOptionsCommonElements() {
        lastValueVisible?.let { put("lastValueVisible", it) }
        title?.let { put("title", it) }
        priceScaleId?.let { put("priceScaleId", it) }
        visible?.let { put("visible", it) }
        priceLineVisible?.let { put("priceLineVisible", it) }
        priceLineSource?.let { put("priceLineSource", it.toJsonElement()) }
        priceLineWidth?.let { put("priceLineWidth", it.toJsonElement()) }
        priceLineColor?.let { put("priceLineColor", it) }
        priceLineStyle?.let { put("priceLineStyle", it.toJsonElement()) }
        priceFormat?.let { put("priceFormat", it.toJsonElement()) }
        baseLineVisible?.let { put("baseLineVisible", it) }
        baseLineColor?.let { put("baseLineColor", it) }
        baseLineWidth?.let { put("baseLineWidth", it.toJsonElement()) }
        baseLineStyle?.let { put("baseLineStyle", it.toJsonElement()) }
    }

    override fun toJsonElement(): JsonElement = buildJsonObject {
        putSeriesOptionsCommonElements()
    }
}
