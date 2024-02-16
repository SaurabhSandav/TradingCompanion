package com.saurabhsandav.core.trading.autotrader.impl

import com.saurabhsandav.core.trading.autotrader.Signal
import com.saurabhsandav.core.trading.autotrader.StrategyInstanceScope
import com.saurabhsandav.core.trading.backtest.BacktestBroker
import java.math.BigDecimal

internal class StrategyInstanceScopeImpl(
    private val ticker: String,
    private val broker: BacktestBroker,
) : StrategyInstanceScope {

    private val signals = mutableListOf<Signal>()

    override val hasOpenPositions: Boolean
        get() = broker.positions.value.any { it.ticker == ticker }

    fun consumeSignals(): List<Signal> {

        val signals = signals.toList()

        this.signals.clear()

        return signals
    }

    override fun cancelAllEntryOrders() {
        signals += Signal.CancelAllEntryOrders
    }

    override fun exitAllPositions() {
        signals += Signal.ExitAllPositions
    }

    override fun buy(
        price: BigDecimal,
        stop: BigDecimal,
        target: BigDecimal,
    ) {

        signals += Signal.Buy(
            price = price,
            stop = stop,
            target = target,
        )
    }

    override fun sell(
        price: BigDecimal,
        stop: BigDecimal,
        target: BigDecimal,
    ) {

        signals += Signal.Sell(
            price = price,
            stop = stop,
            target = target,
        )
    }
}
