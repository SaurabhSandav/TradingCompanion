package com.saurabhsandav.core.trades

import com.saurabhsandav.core.utils.StartupJob

internal class TradeManagementJob(
    private val tradingRecord: TradingRecord,
) : StartupJob {

    override suspend fun run() {

        tradingRecord.trades.generateMfeAndMaeForAllTrades()
    }
}
