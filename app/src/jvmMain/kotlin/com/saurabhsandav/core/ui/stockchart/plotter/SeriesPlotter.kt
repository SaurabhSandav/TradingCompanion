package com.saurabhsandav.core.ui.stockchart.plotter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.saurabhsandav.core.ui.stockchart.Plotter
import com.saurabhsandav.core.ui.stockchart.StockChart
import com.saurabhsandav.lightweightcharts.ISeriesApi
import com.saurabhsandav.lightweightcharts.data.MouseEventParams
import com.saurabhsandav.lightweightcharts.data.SeriesData
import com.saurabhsandav.lightweightcharts.options.SeriesOptions
import com.saurabhsandav.lightweightcharts.options.SeriesOptionsCommon

abstract class SeriesPlotter<D : SeriesData, O : SeriesOptions> : Plotter<D> {

    protected var latestValue: D? = null

    private var _isEnabled by mutableStateOf(true)
    override var isEnabled: Boolean
        get() = _isEnabled
        set(value) {

            _isEnabled = value

            series.applyOptions(SeriesOptionsCommon(visible = value))
        }

    private var _series: ISeriesApi<D, O>? = null
    val series: ISeriesApi<D, O>
        get() = checkNotNull(_series) { "Series not initialized" }

    abstract fun createSeries(chart: StockChart): ISeriesApi<D, O>

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
        latestValue = data.lastOrNull()
    }

    override fun update(item: D) {
        series.update(item)
        latestValue = item
    }

    fun updateLegendValues(params: MouseEventParams?) {
        val seriesData = params?.seriesData?.let(series::getMouseEventDataFrom)
        onUpdateLegendValues(seriesData)
    }

    protected abstract fun onUpdateLegendValues(seriesData: SeriesData?)
}
