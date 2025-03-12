package com.saurabhsandav.lightweight_charts.options

import com.saurabhsandav.lightweight_charts.options.common.LastPriceAnimationMode
import com.saurabhsandav.lightweight_charts.options.common.LineStyle
import com.saurabhsandav.lightweight_charts.options.common.LineType
import com.saurabhsandav.lightweight_charts.options.common.LineWidth
import com.saurabhsandav.lightweight_charts.options.common.PriceFormat
import com.saurabhsandav.lightweight_charts.options.common.PriceLineSource
import kotlinx.serialization.Serializable

@Serializable
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
    val lineType: LineType? = null,
    val lineVisible: Boolean? = null,
    val pointMarkersVisible: Boolean? = null,
    val pointMarkersRadius: Double? = null,
    val crosshairMarkerVisible: Boolean? = null,
    val crosshairMarkerRadius: Double? = null,
    val crosshairMarkerBorderColor: String? = null,
    val crosshairMarkerBackgroundColor: String? = null,
    val crosshairMarkerBorderWidth: Double? = null,
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
) : SeriesOptions {

    @Serializable
    data class BaseValuePrice(
        val type: String,
        val price: Double,
    )
}
