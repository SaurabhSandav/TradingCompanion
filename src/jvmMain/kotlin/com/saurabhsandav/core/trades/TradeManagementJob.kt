package com.saurabhsandav.core.trades

import com.saurabhsandav.core.utils.StartupJob
import kotlinx.coroutines.flow.first

internal class TradeManagementJob(
    private val tradingProfiles: TradingProfiles,
) : StartupJob {

    override suspend fun run() {

        tradingProfiles.allProfiles.first().forEach { profile ->
            tradingProfiles.getRecord(profile.id).trades.generateMfeAndMaeForAllTrades()
        }
    }
}
