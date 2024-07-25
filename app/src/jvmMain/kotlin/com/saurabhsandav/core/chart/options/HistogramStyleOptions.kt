package com.saurabhsandav.core.chart.options

import com.saurabhsandav.core.chart.IsJsonElement
import com.saurabhsandav.core.chart.options.common.LineStyle
import com.saurabhsandav.core.chart.options.common.LineWidth
import com.saurabhsandav.core.chart.options.common.PriceFormat
import com.saurabhsandav.core.chart.options.common.PriceLineSource
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class HistogramStyleOptions(
    val color: String? = null,
    val base: Number? = null,

    override val lastValueVisible: Boolean? = null,
    override val title: String? = null,
    override val priceScaleId: String? = null,
    override val visible: Boolean? = null,
    override val priceLineVisible: Boolean? = null,
    override val priceLineSource: PriceLineSource? = null,
    override val priceLineWidth: LineWidth? = null,
    override val priceLineColor: String? = null,
    override val priceLineStyle: LineStyle? = null,
    override val priceFormat: PriceFormat? = null,
    override val baseLineVisible: Boolean? = null,
    override val baseLineColor: String? = null,
    override val baseLineWidth: LineWidth? = null,
    override val baseLineStyle: LineStyle? = null,
) : SeriesOptionsCommon(), IsJsonElement {

    override fun toJsonElement(): JsonObject = buildJsonObject {

        color?.let { put("color", it) }
        base?.let { put("base", it) }

        putSeriesOptionsCommonElements()
    }
}
