package chart.options

import androidx.compose.ui.graphics.Color
import chart.IsJsonElement
import chart.options.common.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import ui.common.toHexString

data class LineStyleOptions(
    val color: Color? = null,
    val lineStyle: LineStyle? = null,
    val lineWidth: LineWidth? = null,
    val lineType: LineType? = null,
    val crosshairMarkerVisible: Boolean? = null,
    val crosshairMarkerRadius: Number? = null,
    val crosshairMarkerBorderColor: Color? = null,
    val crosshairMarkerBackgroundColor: Color? = null,
    val lastPriceAnimation: LastPriceAnimationMode? = null,

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
        lineType?.let { put("lineType", it.toJsonElement()) }
        crosshairMarkerVisible?.let { put("crosshairMarkerVisible", it) }
        crosshairMarkerRadius?.let { put("crosshairMarkerRadius", it) }
        crosshairMarkerBorderColor?.let { put("crosshairMarkerBorderColor", it.toHexString()) }
        crosshairMarkerBackgroundColor?.let { put("crosshairMarkerBackgroundColor", it.toHexString()) }
        lastPriceAnimation?.let { put("lastPriceAnimation", it.toJsonElement()) }

        putSeriesOptionsCommonElements()
    }
}
