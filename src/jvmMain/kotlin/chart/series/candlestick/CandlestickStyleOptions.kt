package chart.series.candlestick

import chart.series.SeriesOptionsCommon
import chart.series.SeriesOptionsCommon.Companion.putSeriesOptionsCommonElements
import chart.misc.LineStyle
import chart.misc.LineWidth
import chart.misc.PriceFormat
import chart.pricescale.PriceLineSource
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class CandlestickStyleOptions(
    val upColor: String? = null,
    val downColor: String? = null,
    val wickVisible: Boolean? = null,
    val borderVisible: Boolean? = null,
    val borderColor: String? = null,
    val borderUpColor: String? = null,
    val borderDownColor: String? = null,
    val wickColor: String? = null,
    val wickUpColor: String? = null,
    val wickDownColor: String? = null,

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
) : SeriesOptionsCommon {

    fun toJsonObject(): JsonObject = buildJsonObject {

        upColor?.let { put("upColor", it) }
        downColor?.let { put("downColor", it) }
        wickVisible?.let { put("wickVisible", it) }
        borderVisible?.let { put("borderVisible", it) }
        borderColor?.let { put("borderColor", it) }
        borderUpColor?.let { put("borderUpColor", it) }
        borderDownColor?.let { put("borderDownColor", it) }
        wickColor?.let { put("wickColor", it) }
        wickUpColor?.let { put("wickUpColor", it) }
        wickDownColor?.let { put("wickDownColor", it) }

        putSeriesOptionsCommonElements(this@CandlestickStyleOptions)
    }
}
