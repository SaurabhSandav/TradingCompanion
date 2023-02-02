package com.saurabhsandav.core.chart.options

import com.saurabhsandav.core.chart.IsJsonElement
import com.saurabhsandav.core.chart.options.common.LineStyle
import com.saurabhsandav.core.chart.options.common.LineWidth
import com.saurabhsandav.core.chart.options.common.PriceFormat
import com.saurabhsandav.core.chart.options.common.PriceLineSource
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.put

interface SeriesOptions : IsJsonElement

abstract class SeriesOptionsCommon {

    abstract val lastValueVisible: Boolean?

    abstract val title: String?

    abstract val priceScaleId: String?

    abstract val visible: Boolean?

    abstract val priceLineVisible: Boolean?

    abstract val priceLineSource: PriceLineSource?

    abstract val priceLineWidth: LineWidth?

    abstract val priceLineColor: String?

    abstract val priceLineStyle: LineStyle?

    abstract val priceFormat: PriceFormat?

    abstract val baseLineVisible: Boolean?

    abstract val baseLineColor: String?

    abstract val baseLineWidth: LineWidth?

    abstract val baseLineStyle: LineStyle?

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
}
