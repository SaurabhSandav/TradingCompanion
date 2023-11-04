package com.saurabhsandav.core.ui.stockchart.plotter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.saurabhsandav.core.chart.ISeriesApi
import com.saurabhsandav.core.chart.data.SeriesData
import com.saurabhsandav.core.chart.data.SeriesMarker
import com.saurabhsandav.core.chart.options.SeriesOptionsCommon
import com.saurabhsandav.core.ui.stockchart.Plotter
import com.saurabhsandav.core.ui.stockchart.StockChart

abstract class SeriesPlotter<D : SeriesData> : Plotter<D> {

    private var _isEnabled by mutableStateOf(true)
    override var isEnabled: Boolean
        get() = _isEnabled
        set(value) {

            _isEnabled = value

            series.applyOptions(SeriesOptionsCommon(visible = value))
        }

    private var _series: ISeriesApi<D>? = null
    val series: ISeriesApi<D>
        get() = checkNotNull(_series) { "Series not initialized" }

    abstract fun createSeries(chart: StockChart): ISeriesApi<D>

    override fun onAttach(chart: StockChart) {
        check(_series == null) { "Plotter already attached" }
        _series = createSeries(chart)
    }

    override fun onDetach(chart: StockChart) {
        chart.actualChart.removeSeries(series)
        _series = null
    }

    override fun setData(data: List<D>) {
        series.setData(data)
    }

    override fun update(item: D) {
        series.update(item)
    }

    fun setMarkers(markers: List<SeriesMarker>) {
        series.setMarkers(markers)
    }
}
