package com.saurabhsandav.lightweight_charts.options

import com.saurabhsandav.lightweight_charts.options.common.LineStyle
import com.saurabhsandav.lightweight_charts.options.common.LineWidth
import com.saurabhsandav.lightweight_charts.options.common.PriceFormat
import com.saurabhsandav.lightweight_charts.options.common.PriceLineSource
import kotlinx.serialization.Serializable

@Serializable
data class HistogramStyleOptions(
    val color: String? = null,
    val base: Double? = null,

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
