package com.saurabhsandav.core.ui.stockchart

import com.saurabhsandav.core.chart.data.SeriesMarker
import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.Timeframe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

interface CandleSource {

    val ticker: String

    val timeframe: Timeframe

    val hasVolume: Boolean

    val candleSeries: CandleSeries

    val candleMarkers: Flow<List<SeriesMarker>>
        get() = emptyFlow()

    suspend fun onLoad()

    suspend fun onLoadBefore(): Boolean = false

    suspend fun onLoadAfter(): Boolean = false
}
