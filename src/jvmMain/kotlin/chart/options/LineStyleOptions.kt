package chart.options

import androidx.compose.ui.graphics.Color
import chart.IsJsonElement
import chart.options.common.LineStyle
import chart.options.common.LineWidth
import chart.options.common.PriceFormat
import chart.options.common.PriceLineSource
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import ui.common.toHexString

data class LineStyleOptions(
    val color: Color? = null,
    val lineStyle: LineStyle? = null,
    val lineWidth: LineWidth? = null,

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
) : SeriesOptionsCommon(), SeriesOptions, IsJsonElement {

    override fun toJsonElement(): JsonObject = buildJsonObject {

        color?.let { put("color", it.toHexString()) }
        lineStyle?.let { put("lineStyle", it.toJsonElement()) }
        lineWidth?.let { put("lineWidth", it.toJsonElement()) }

        putSeriesOptionsCommonElements()
    }
}
