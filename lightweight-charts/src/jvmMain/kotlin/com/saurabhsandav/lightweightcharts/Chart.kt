package com.saurabhsandav.lightweightcharts

import com.saurabhsandav.lightweightcharts.data.SeriesData
import com.saurabhsandav.lightweightcharts.options.BaselineStyleOptions
import com.saurabhsandav.lightweightcharts.options.CandlestickStyleOptions
import com.saurabhsandav.lightweightcharts.options.ChartOptions
import com.saurabhsandav.lightweightcharts.options.HistogramStyleOptions
import com.saurabhsandav.lightweightcharts.options.LineStyleOptions
import com.saurabhsandav.lightweightcharts.options.SeriesOptions
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun createChart(
    container: String = "document.body",
    options: ChartOptions? = null,
    id: String = "chart",
): IChartApi = IChartApi(container, options, id)

fun IChartApi.baselineSeries(options: BaselineStyleOptions? = null) =
    SeriesProvider { name -> addSeries(SeriesDefinition.BaselineSeries, name, options) }

fun IChartApi.candlestickSeries(options: CandlestickStyleOptions? = null) =
    SeriesProvider { name -> addSeries(SeriesDefinition.CandlestickSeries, name, options) }

fun IChartApi.histogramSeries(options: HistogramStyleOptions? = null) =
    SeriesProvider { name -> addSeries(SeriesDefinition.HistogramSeries, name, options) }

fun IChartApi.lineSeries(options: LineStyleOptions? = null) =
    SeriesProvider { name -> addSeries(SeriesDefinition.LineSeries, name, options) }

class SeriesProvider<D : SeriesData, O : SeriesOptions>(
    private val seriesBuilder: (propertyName: String) -> ISeriesApi<D, O>,
) {

    private var series: ISeriesApi<D, O>? = null

    operator fun provideDelegate(
        thisRef: Any?,
        prop: KProperty<*>,
    ): ReadOnlyProperty<Any?, ISeriesApi<D, O>> {

        series = seriesBuilder(prop.name)

        return ReadOnlyProperty { _, _ -> series!! }
    }
}
