package com.saurabhsandav.core.ui.stockchart.plotter

import com.saurabhsandav.core.chart.IChartApi
import com.saurabhsandav.core.chart.ISeriesApi
import com.saurabhsandav.core.chart.data.SeriesData
import com.saurabhsandav.core.chart.misc.MouseEventParams

abstract class SeriesPlotter<T : SeriesData>(
    private val chart: IChartApi,
    private val mapper: (index: Int) -> T,
) {

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

    fun setData(range: IntRange) {

        if (!isEnabled()) return

        if (series == null)
            series = createSeries()

        val seriesData = range.map { index -> mapper(index) }

        checkNotNull(series).setData(seriesData)
    }

    fun update(index: Int) {

        val series = checkNotNull(series)

        series.update(mapper(index))
    }
}
