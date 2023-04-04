package com.saurabhsandav.core.trades

import com.saurabhsandav.core.utils.StartupJob

internal class TradeManagementJob(
    private val tradesRepo: TradesRepo,
) : StartupJob {

    override suspend fun run() {

        tradesRepo.generateMfeAndMaeForAllTrades()
    }
}
