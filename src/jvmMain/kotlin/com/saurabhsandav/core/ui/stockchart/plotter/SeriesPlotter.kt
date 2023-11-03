package com.saurabhsandav.core.ui.stockchart.plotter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.saurabhsandav.core.chart.ISeriesApi
import com.saurabhsandav.core.chart.data.SeriesData
import com.saurabhsandav.core.chart.data.SeriesMarker
import com.saurabhsandav.core.chart.options.SeriesOptionsCommon
import com.saurabhsandav.core.ui.stockchart.StockChart
import kotlinx.coroutines.flow.Flow

abstract class SeriesPlotter<T : SeriesData> {

    abstract val key: String

    abstract val legendLabel: String

    private var _isEnabled by mutableStateOf(true)
    var isEnabled: Boolean
        get() = _isEnabled
        set(value) {

            _isEnabled = value

            series.applyOptions(SeriesOptionsCommon(visible = value))
        }

    private var dataSource: DataSource<T>? = null

    private var _series: ISeriesApi<T>? = null
    val series: ISeriesApi<T>
        get() = checkNotNull(_series) { "Series not initialized" }

    abstract fun createSeries(chart: StockChart): ISeriesApi<T>

    abstract fun legendText(chart: StockChart): Flow<String>

    fun onAttach(chart: StockChart) {
        check(_series == null) { "Plotter already attached" }
        _series = createSeries(chart)
    }

    fun onDetach(chart: StockChart) {
        chart.actualChart.removeSeries(series)
        _series = null
    }

    fun setDataSource(source: DataSource<T>?) {
        dataSource = source
    }

    fun setData(range: IntRange) {

        val seriesData = when (val dataSource = dataSource) {
            null -> emptyList()
            else -> range.map(dataSource::getValue)
        }

        series.setData(seriesData)
    }

    fun update(index: Int) {

        dataSource?.let { dataSource ->
            series.update(dataSource.getValue(index))
        }
    }

    fun setMarkers(markers: List<SeriesMarker>) {
        series.setMarkers(markers)
    }

    fun interface DataSource<T> {

        fun getValue(index: Int): T
    }
}
