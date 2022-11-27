package chart

import chart.data.SeriesData
import chart.options.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun createChart(
    container: String = "document.body",
    options: ChartOptions = ChartOptions(),
    name: String = "chart",
): IChartApi = IChartApi(container, options, name)

fun IChartApi.baselineSeries(
    options: BaselineStyleOptions = BaselineStyleOptions(),
) = SeriesProvider { name -> addBaselineSeries(options, name) }

fun IChartApi.candlestickSeries(
    options: CandlestickStyleOptions = CandlestickStyleOptions(),
) = SeriesProvider { name -> addCandlestickSeries(options, name) }

fun IChartApi.histogramSeries(
    options: HistogramStyleOptions = HistogramStyleOptions(),
) = SeriesProvider { name -> addHistogramSeries(options, name) }

fun IChartApi.lineSeries(
    options: LineStyleOptions = LineStyleOptions(),
) = SeriesProvider { name -> addLineSeries(options, name) }

class SeriesProvider<T : SeriesData>(
    private val seriesBuilder: (propertyName: String) -> ISeriesApi<T>,
) {

    private var series: ISeriesApi<T>? = null

    operator fun provideDelegate(
        thisRef: Any?,
        prop: KProperty<*>,
    ): ReadOnlyProperty<Any?, ISeriesApi<T>> {

        series = seriesBuilder(prop.name)

        return ReadOnlyProperty { _, _ -> series!! }
    }
}
