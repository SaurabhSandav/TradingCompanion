package com.saurabhsandav.lightweightcharts

import com.saurabhsandav.lightweightcharts.data.BaselineData
import com.saurabhsandav.lightweightcharts.data.CandlestickData
import com.saurabhsandav.lightweightcharts.data.HistogramData
import com.saurabhsandav.lightweightcharts.data.LineData
import com.saurabhsandav.lightweightcharts.data.SeriesData
import com.saurabhsandav.lightweightcharts.options.BaselineStyleOptions
import com.saurabhsandav.lightweightcharts.options.CandlestickStyleOptions
import com.saurabhsandav.lightweightcharts.options.HistogramStyleOptions
import com.saurabhsandav.lightweightcharts.options.LineStyleOptions
import com.saurabhsandav.lightweightcharts.options.SeriesOptions
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
