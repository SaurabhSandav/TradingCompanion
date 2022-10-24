package chart.options

import chart.IsJsonElement
import chart.options.common.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class BaselineStyleOptions(
    val baseValue: BaseValuePrice? = null,
    val topFillColor1: String? = null,
    val topFillColor2: String? = null,
    val topLineColor: String? = null,
    val bottomFillColor1: String? = null,
    val bottomFillColor2: String? = null,
    val bottomLineColor: String? = null,
    val lineWidth: LineWidth? = null,
    val lineStyle: LineStyle? = null,
    val crosshairMarkerVisible: Boolean? = null,
    val crosshairMarkerRadius: Number? = null,
    val crosshairMarkerBorderColor: String? = null,
    val crosshairMarkerBackgroundColor: String? = null,
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

        baseValue?.let { put("baseValue", it.toJsonElement()) }
        topFillColor1?.let { put("topFillColor1", it) }
        topFillColor2?.let { put("topFillColor2", it) }
        topLineColor?.let { put("topLineColor", it) }
        bottomFillColor1?.let { put("bottomFillColor1", it) }
        bottomFillColor2?.let { put("bottomFillColor2", it) }
        bottomLineColor?.let { put("bottomLineColor", it) }
        lineWidth?.let { put("lineWidth", it.toJsonElement()) }
        lineStyle?.let { put("lineStyle", it.toJsonElement()) }
        crosshairMarkerVisible?.let { put("crosshairMarkerVisible", it) }
        crosshairMarkerRadius?.let { put("crosshairMarkerRadius", it) }
        crosshairMarkerBorderColor?.let { put("crosshairMarkerBorderColor", it) }
        crosshairMarkerBackgroundColor?.let { put("crosshairMarkerBackgroundColor", it) }
        lastPriceAnimation?.let { put("lastPriceAnimation", it.toJsonElement()) }

        putSeriesOptionsCommonElements()
    }
}

data class BaseValuePrice(
    val type: String,
    val price: Number,
) : IsJsonElement {

    override fun toJsonElement(): JsonObject = buildJsonObject {
        put("type", type)
        put("price", price)
    }
}
