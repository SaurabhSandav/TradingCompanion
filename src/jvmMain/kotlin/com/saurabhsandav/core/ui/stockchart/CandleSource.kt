package com.saurabhsandav.core.ui.stockchart

import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.Timeframe

interface CandleSource {

    val ticker: String

    val timeframe: Timeframe

    val hasVolume: Boolean

    val candleSeries: CandleSeries

    suspend fun onLoad()

    suspend fun onLoadBefore(): Boolean = false

    suspend fun onLoadAfter(): Boolean = false
}
