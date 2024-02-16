package com.saurabhsandav.core.trading.autotrader

import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.Timeframe
import java.math.BigDecimal

fun Strategy(
    title: String,
    block: suspend StrategyInitScope.() -> Strategy.Instance,
): Strategy = object : Strategy {

    override val title: String = title

    override suspend fun StrategyInitScope.init() = block()
}

interface Strategy {

    val title: String

    suspend fun StrategyInitScope.init(): Instance

    fun interface Instance {

        fun StrategyInstanceScope.onNewEvent()
    }
}

interface StrategyInitScope {

    suspend fun getCandleSeries(timeframe: Timeframe): CandleSeries

    suspend fun getCandleSeries(
        ticker: String,
        timeframe: Timeframe,
    ): CandleSeries
}

interface StrategyInstanceScope {

    val hasOpenPositions: Boolean

    fun cancelAllEntryOrders()

    fun exitAllPositions()

    fun buy(
        price: BigDecimal,
        stop: BigDecimal,
        target: BigDecimal,
    )

    fun sell(
        price: BigDecimal,
        stop: BigDecimal,
        target: BigDecimal,
    )
}
