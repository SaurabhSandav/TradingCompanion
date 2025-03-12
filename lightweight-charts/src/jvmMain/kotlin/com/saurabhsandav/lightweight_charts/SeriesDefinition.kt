package com.saurabhsandav.lightweight_charts

import com.saurabhsandav.lightweight_charts.data.BaselineData
import com.saurabhsandav.lightweight_charts.data.CandlestickData
import com.saurabhsandav.lightweight_charts.data.HistogramData
import com.saurabhsandav.lightweight_charts.data.LineData
import com.saurabhsandav.lightweight_charts.data.SeriesData
import com.saurabhsandav.lightweight_charts.options.BaselineStyleOptions
import com.saurabhsandav.lightweight_charts.options.CandlestickStyleOptions
import com.saurabhsandav.lightweight_charts.options.HistogramStyleOptions
import com.saurabhsandav.lightweight_charts.options.LineStyleOptions
import com.saurabhsandav.lightweight_charts.options.SeriesOptions
import kotlinx.serialization.KSerializer

sealed class SeriesDefinition<D : SeriesData, O : SeriesOptions>(
    internal val jsStatement: String,
) {

    internal abstract val dataSerializer: KSerializer<D>
    internal abstract val optionsSerializer: KSerializer<O>

    data object BaselineSeries :
        SeriesDefinition<BaselineData, BaselineStyleOptions>("LightweightCharts.BaselineSeries") {

        override val dataSerializer = BaselineData.serializer()
        override val optionsSerializer = BaselineStyleOptions.serializer()
    }

    data object CandlestickSeries :
        SeriesDefinition<CandlestickData, CandlestickStyleOptions>("LightweightCharts.CandlestickSeries") {

        override val dataSerializer = CandlestickData.serializer()
        override val optionsSerializer = CandlestickStyleOptions.serializer()
    }

    data object HistogramSeries :
        SeriesDefinition<HistogramData, HistogramStyleOptions>("LightweightCharts.HistogramSeries") {

        override val dataSerializer = HistogramData.serializer()
        override val optionsSerializer = HistogramStyleOptions.serializer()
    }

    data object LineSeries :
        SeriesDefinition<LineData, LineStyleOptions>("LightweightCharts.LineSeries") {

        override val dataSerializer = LineData.serializer()
        override val optionsSerializer = LineStyleOptions.serializer()
    }
}
