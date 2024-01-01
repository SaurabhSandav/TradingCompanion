package com.saurabhsandav.core

import com.saurabhsandav.core.di.AppModule
import com.saurabhsandav.core.trades.brokerageAt
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import java.math.BigDecimal

internal class TradeSimulator(
    private val appModule: AppModule,
) {

    fun run() = MainScope().launch {

        val profile = appModule.tradingProfiles.getDefaultProfile().first()
        val record = appModule.tradingProfiles.getRecord(profile.id)
        val tradesRepo = record.trades

        var currentTotalPnl = BigDecimal.ZERO
        var targetTotalPnl = BigDecimal.ZERO

        val trades = tradesRepo.allTrades.first()

        val afterInstant = LocalDate(year = 2023, monthNumber = 11, dayOfMonth = 1)
            .atStartOfDayIn(TimeZone.currentSystemDefault())

        trades
            .filter { it.entryTimestamp > afterInstant && it.isClosed }
            .forEach { trade ->

                val stop = record.stops.getPrimary(trade.id).first() ?: return@forEach
                val target = record.targets.getPrimary(trade.id).first() ?: return@forEach
                val excursions = record.excursions.get(trade.id).first() ?: return@forEach

                val targetPnl = trade.brokerageAt(target)
                val stopPnl = trade.brokerageAt(stop)

                currentTotalPnl += trade.pnl

                targetTotalPnl += when {
                    excursions.sessionMfePnl >= targetPnl.pnl -> targetPnl.pnl
                    excursions.sessionMaePnl <= stopPnl.pnl -> stopPnl.pnl
                    else -> trade.pnl
                }
            }

        val diff = targetTotalPnl - currentTotalPnl

        println("Current total PNL: $currentTotalPnl, Target total PNL: $targetTotalPnl, Diff: $diff")
    }
}
