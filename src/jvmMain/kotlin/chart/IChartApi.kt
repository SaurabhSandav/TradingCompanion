package chart

import chart.baseline.BaselineSeries
import chart.candlestick.CandlestickSeries
import chart.histogram.HistogramSeries
import chart.timescale.TimeScale

internal class IChartApi(
    private val executeJs: (String) -> Unit,
    val name: String = "chart",
) {

    init {

        executeJs(
            """
                |const $name = LightweightCharts.createChart(document.body, {
                |    width: window.innerWidth,
                |    height: window.innerHeight
                |});
            """.trimMargin()
        )
    }

    val timeScale = TimeScale(this, executeJs)

    fun addBaselineSeries(name: String = "baselineSeries"): BaselineSeries {

        val series = BaselineSeries(executeJs, name)

        executeJs(
            """
                |var ${series.name} = (typeof ${series.name} != "undefined") ? ${series.name} : ${this@IChartApi.name}.addBaselineSeries({
                |    baseValue: {
                |        type: 'price',
                |        price: 0
                |    },
                |    topLineColor: 'rgba( 38, 166, 154, 1)',
                |    topFillColor1: 'rgba( 38, 166, 154, 0.28)',
                |    topFillColor2: 'rgba( 38, 166, 154, 0.05)',
                |    bottomLineColor: 'rgba( 239, 83, 80, 1)',
                |    bottomFillColor1: 'rgba( 239, 83, 80, 0.05)',
                |    bottomFillColor2: 'rgba( 239, 83, 80, 0.28)'
                |});
            """.trimMargin()
        )

        return series
    }

    fun addCandlestickSeries(name: String = "candlestickSeries"): CandlestickSeries {

        val series = CandlestickSeries(executeJs, name)

        executeJs(
            """
                |
                |var ${series.name} = (typeof ${series.name} != "undefined") ? ${series.name} : ${this@IChartApi.name}.addCandlestickSeries({
                |    upColor: '#26a69a',
                |    downColor: '#ef5350',
                |    borderVisible: false,
                |    wickUpColor: '#26a69a',
                |    wickDownColor: '#ef5350'
                |});
            """.trimMargin()
        )

        return series
    }

    fun addHistogramSeries(name: String = "histogramSeries"): HistogramSeries {

        val series = HistogramSeries(executeJs, name)

        executeJs(
            """
                |var ${series.name} = (typeof ${series.name} != "undefined") ? ${series.name} : ${this@IChartApi.name}.addHistogramSeries({
                |    color: '#26a69a',
                |    priceFormat: {
                |        type: 'volume',
                |    },
                |    priceScaleId: '',
                |    scaleMargins: {
                |        top: 0.8,
                |        bottom: 0,
                |    },
                |});
            """.trimMargin()
        )

        return series
    }

    fun resize(width: Int, height: Int) {
        executeJs("$name.resize($width, $height)")
    }

    fun removeSeries(series: ISeriesApi<*>) {
        executeJs("$name.removeSeries(${series.name});")
    }
}
