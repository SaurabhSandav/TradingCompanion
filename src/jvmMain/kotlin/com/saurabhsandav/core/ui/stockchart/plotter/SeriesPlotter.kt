package com.saurabhsandav.core.ui.stockchart.plotter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.saurabhsandav.core.chart.IChartApi
import com.saurabhsandav.core.chart.ISeriesApi
import com.saurabhsandav.core.chart.data.SeriesData
import com.saurabhsandav.core.chart.data.SeriesMarker
import com.saurabhsandav.core.chart.misc.MouseEventParams
import com.saurabhsandav.core.chart.options.SeriesOptionsCommon

abstract class SeriesPlotter<T : SeriesData>(
    private val chart: IChartApi,
) {

    abstract val key: String

    abstract val legendLabel: String

    private var dataSource: DataSource<T>? = null

    var series: ISeriesApi<T>? = null
        private set

    var isEnabled by mutableStateOf(true)
        private set

    abstract fun legendText(params: MouseEventParams): String

    protected abstract fun createSeries(): ISeriesApi<T>

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

        if (series == null) {
            series = createSeries()
            setIsEnabled(isEnabled)
        }

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

    fun setMarkers(markers: List<SeriesMarker>) {
        series?.setMarkers(markers)
    }

    fun setIsEnabled(enabled: Boolean) {

        isEnabled = enabled

        series?.applyOptions(SeriesOptionsCommon(visible = enabled))
    }

    fun interface DataSource<T> {

        fun getValue(index: Int): T
    }
}
