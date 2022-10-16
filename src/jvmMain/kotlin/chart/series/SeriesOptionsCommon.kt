package chart.series

import chart.misc.LineStyle
import chart.misc.LineWidth
import chart.series.pricescale.PriceFormat
import chart.series.pricescale.PriceLineSource
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.put

interface SeriesOptionsCommon {

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

    companion object {

        internal fun JsonObjectBuilder.putSeriesOptionsCommonElements(options: SeriesOptionsCommon) = with(options) {
            lastValueVisible?.let { put("lastValueVisible", it) }
            title?.let { put("title", it) }
            priceScaleId?.let { put("priceScaleId", it) }
            visible?.let { put("visible", it) }
            priceLineVisible?.let { put("priceLineVisible", it) }
            priceLineSource?.let { put("priceLineSource", it.strValue) }
            priceLineWidth?.let { put("priceLineWidth", it.intValue) }
            priceLineColor?.let { put("priceLineColor", it) }
            priceLineStyle?.let { put("priceLineStyle", it.strValue) }
            priceFormat?.let { put("priceFormat", it.toJsonObject()) }
            baseLineVisible?.let { put("baseLineVisible", it) }
            baseLineColor?.let { put("baseLineColor", it) }
            baseLineWidth?.let { put("baseLineWidth", it.intValue) }
            baseLineStyle?.let { put("baseLineStyle", it.strValue) }
        }
    }
}
