package com.saurabhsandav.lightweight_charts.options

import com.saurabhsandav.lightweight_charts.options.common.*
import com.saurabhsandav.lightweight_charts.utils.SerializableColor
import kotlinx.serialization.Serializable

@Serializable
data class LineStyleOptions(
    val color: SerializableColor? = null,
    val lineStyle: LineStyle? = null,
    val lineWidth: LineWidth? = null,
    val lineType: LineType? = null,
    val lineVisible: Boolean? = null,
    val pointMarkersVisible: Boolean? = null,
    val pointMarkersRadius: Double? = null,
    val crosshairMarkerVisible: Boolean? = null,
    val crosshairMarkerRadius: Double? = null,
    val crosshairMarkerBorderColor: SerializableColor? = null,
    val crosshairMarkerBackgroundColor: SerializableColor? = null,
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
) : SeriesOptions
