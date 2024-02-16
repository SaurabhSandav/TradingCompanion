package com.saurabhsandav.core.trading.autotrader

import com.saurabhsandav.core.trades.TradingRecord
import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.autotrader.impl.StrategyInstanceScopeImpl
import com.saurabhsandav.core.trading.backtest.BacktestBroker
import com.saurabhsandav.core.trading.backtest.BacktestExecution
import com.saurabhsandav.core.trading.backtest.newCandle
import com.saurabhsandav.core.trading.sizing.PositionSizer

internal class StrategyRunner(
    private val m1Series: CandleSeries,
    private val ticker: String,
    orderTypeToExecutionType: OrderTypeToExecutionType,
    sizer: PositionSizer,
    private val broker: BacktestBroker,
    private val record: TradingRecord,
    private val strategyInstance: Strategy.Instance,
) {

    private val signalsManager = SignalsManager(ticker, orderTypeToExecutionType, sizer, broker, record)
    private val runnerScope = StrategyInstanceScopeImpl(ticker, broker)
    private var lastLoggedExecutionId = -1L

    suspend fun onAdvance() {

        val candle = m1Series.last()

        // Update broker with latest price
        broker.newCandle(
            ticker = ticker,
            candle = candle,
            replayOHLC = false,
        )

        // Save Executions to DB
        broker.executions.value
            .takeLastWhile { it.id.value != lastLoggedExecutionId }
            .filter { it.ticker == ticker }
            .forEach { execution ->
                record.saveExecutionToDB(execution)
                lastLoggedExecutionId = execution.id.value
            }

        // Handle order updates
        signalsManager.onEvent()

        // Run strategy
        with(strategyInstance) {
            runnerScope.onNewEvent()
        }

        // Handle signals
        runnerScope.consumeSignals().forEach(signalsManager::processSignal)
    }

    private suspend fun TradingRecord.saveExecutionToDB(execution: BacktestExecution) {

        executions.new(
            broker = execution.broker,
            instrument = execution.instrument,
            ticker = execution.ticker,
            quantity = execution.quantity,
            lots = null,
            side = execution.side,
            price = execution.price,
            timestamp = execution.timestamp,
            locked = true,
        )
    }
}
