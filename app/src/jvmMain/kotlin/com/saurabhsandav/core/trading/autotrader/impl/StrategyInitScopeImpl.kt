package com.saurabhsandav.core.trading.autotrader.impl

import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.trading.autotrader.StrategyInitScope

internal class StrategyInitScopeImpl(
    private val ticker: String,
    private val getCandleSeries: suspend (String, Timeframe) -> CandleSeries,
) : StrategyInitScope {

    override suspend fun getCandleSeries(timeframe: Timeframe): CandleSeries {
        return getCandleSeries.invoke(ticker, timeframe)
    }

    override suspend fun getCandleSeries(
        ticker: String,
        timeframe: Timeframe,
    ): CandleSeries {
        return getCandleSeries.invoke(ticker, timeframe)
    }
}
