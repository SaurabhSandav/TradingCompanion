package com.saurabhsandav.core.ui.stockchart.plotter

import com.saurabhsandav.core.chart.IChartApi
import com.saurabhsandav.core.chart.ISeriesApi
import com.saurabhsandav.core.chart.data.SeriesData
import com.saurabhsandav.core.chart.misc.MouseEventParams

abstract class SeriesPlotter<T : SeriesData>(
    private val chart: IChartApi,
) {

    private var dataSource: DataSource<T>? = null

    var series: ISeriesApi<T>? = null
        private set

    abstract fun legendText(params: MouseEventParams): String

    protected abstract fun createSeries(): ISeriesApi<T>

    protected open val isEnabled: () -> Boolean = { true }

    fun remove() {

        val series = series

        if (series != null) {
            chart.removeSeries(series)
            this.series = null
        }
    }

    fun setDataSource(source: DataSource<T>?) {
        dataSource = source
    }

    fun setData(range: IntRange) {

        if (!isEnabled()) return

        if (series == null)
            series = createSeries()

        val seriesData = when (val dataSource = dataSource) {
            null -> emptyList()
            else -> range.map(dataSource::getValue)
        }

        series!!.setData(seriesData)
    }

    fun update(index: Int) {

        val series = checkNotNull(series)

        dataSource?.let { dataSource ->
            series.update(dataSource.getValue(index))
        }
    }

    fun interface DataSource<T> {

        fun getValue(index: Int): T
    }
}
