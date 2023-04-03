package com.saurabhsandav.core.ui.stockchart

import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.ui.stockchart.plotter.SeriesMarker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.datetime.Instant

interface CandleSource {

    val ticker: String

    val timeframe: Timeframe

    val hasVolume: Boolean

    val candleSeries: CandleSeries

    val syncKey: Any?
        get() = null

    val candleMarkers: Flow<List<SeriesMarker>>
        get() = emptyFlow()

    suspend fun onLoad()

    suspend fun onLoad(start: Instant, end: Instant? = null): Boolean = false

    suspend fun onLoadBefore(): Boolean = false

    suspend fun onLoadAfter(): Boolean = false
}
