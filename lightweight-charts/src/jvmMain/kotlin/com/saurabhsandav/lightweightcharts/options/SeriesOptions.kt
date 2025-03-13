package com.saurabhsandav.lightweightcharts.options

import com.saurabhsandav.lightweightcharts.options.common.LineStyle
import com.saurabhsandav.lightweightcharts.options.common.LineWidth
import com.saurabhsandav.lightweightcharts.options.common.PriceFormat
import com.saurabhsandav.lightweightcharts.options.common.PriceLineSource
import kotlinx.serialization.Serializable

interface SeriesOptions {

    val lastValueVisible: Boolean?
    val title: String?
    val priceScaleId: String?
    val visible: Boolean?
    val priceLineVisible: Boolean?
    val priceLineSource: PriceLineSource?
    val priceLineWidth: LineWidth?
    val priceLineColor: String?
    val priceLineStyle: LineStyle?
    val priceFormat: PriceFormat?
    val baseLineVisible: Boolean?
    val baseLineColor: String?
    val baseLineWidth: LineWidth?
    val baseLineStyle: LineStyle?
}

@Serializable
class SeriesOptionsCommon(
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
