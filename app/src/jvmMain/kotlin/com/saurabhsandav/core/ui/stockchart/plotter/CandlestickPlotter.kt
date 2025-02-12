package com.saurabhsandav.core.ui.stockchart.plotter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.saurabhsandav.core.ui.common.hex
import com.saurabhsandav.core.ui.stockchart.StockChart
import com.saurabhsandav.lightweight_charts.ISeriesApi
import com.saurabhsandav.lightweight_charts.SeriesDefinition
import com.saurabhsandav.lightweight_charts.data.CandlestickData
import com.saurabhsandav.lightweight_charts.data.SeriesData
import com.saurabhsandav.lightweight_charts.options.CandlestickStyleOptions
import com.saurabhsandav.lightweight_charts.options.common.PriceFormat

class CandlestickPlotter(
    override val key: String,
) : SeriesPlotter<CandlestickData, CandlestickStyleOptions>() {

    override var legendLabel by mutableStateOf(AnnotatedString(""))

    override var legendText by mutableStateOf(AnnotatedString(""))
        private set

    override fun createSeries(chart: StockChart): ISeriesApi<CandlestickData, CandlestickStyleOptions> {

        val options = CandlestickStyleOptions(
            lastValueVisible = false,
            priceFormat = PriceFormat.BuiltIn(
                type = PriceFormat.Type.Price,
                minMove = 0.05,
            ),
        )

        return chart.actualChart.addSeries(
            definition = SeriesDefinition.CandlestickSeries,
            id = key,
            options = options,
        )
    }

    override fun onUpdateLegendValues(seriesData: SeriesData?) {

        val candlestickData = (seriesData ?: latestValue) as? CandlestickData.Item

        legendText = when (candlestickData) {
            null -> AnnotatedString(" O H L C")
            else -> {

                val color = candlestickData.color?.let { Color.hex(it.value) }
                val valueStyle = SpanStyle(color = color ?: Color.Unspecified)

                buildAnnotatedString {
                    append(" O ")
                    withStyle(valueStyle) { append(candlestickData.open.toString()) }
                    append(" H ")
                    withStyle(valueStyle) { append(candlestickData.high.toString()) }
                    append(" L ")
                    withStyle(valueStyle) { append(candlestickData.low.toString()) }
                    append(" C ")
                    withStyle(valueStyle) { append(candlestickData.close.toString()) }
                }
            }
        }
    }
}
