package com.saurabhsandav.core.ui.studies.impl

import androidx.compose.material3.Text
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.brokerageAt
import com.saurabhsandav.core.trades.brokerageAtExit
import com.saurabhsandav.core.trades.model.TradeSide
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.table.TableSchema
import com.saurabhsandav.core.ui.common.table.addColumn
import com.saurabhsandav.core.ui.common.table.addColumnText
import com.saurabhsandav.core.ui.common.table.tableSchema
import com.saurabhsandav.core.utils.getCurrentTradingRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.math.RoundingMode

internal class PNLByTickerStudy(
    appPrefs: FlowSettings,
    tradingProfiles: TradingProfiles,
) : TableStudy<PNLByTickerStudy.Model>() {

    private val tradesRepo = appPrefs
        .getCurrentTradingRecord(tradingProfiles)
        .map { record -> record.trades }

    override val schema: TableSchema<Model> = tableSchema {
        addColumnText("Ticker") { it.ticker }
        addColumnText("Trades") { it.noOfTrades }
        addColumn("PNL") {
            Text(it.pnl, color = if (it.isProfitable) AppColor.ProfitGreen else AppColor.LossRed)
        }
        addColumn("Net PNL") {
            Text(it.netPnl, color = if (it.isNetProfitable) AppColor.ProfitGreen else AppColor.LossRed)
        }
        addColumnText("Fees") { it.fees }
        addColumn("R") {
            Text(it.rValue, color = if (it.isProfitable) AppColor.ProfitGreen else AppColor.LossRed)
        }
    }

    override val data: Flow<List<Model>> = tradesRepo.flatMapLatest { tradesRepo ->

        tradesRepo.allTrades.map { trades ->

            trades
                .groupBy { it.ticker }
                .map { entries ->

                    val tickerStats = entries.value.filter { it.isClosed }.map { trade ->

                        val brokerage = trade.brokerageAtExit()!!
                        val pnlBD = brokerage.pnl
                        val netPnlBD = brokerage.netPNL

                        val stop = tradesRepo.getStopsForTrade(trade.id).map { tradeStops ->
                            tradeStops.maxByOrNull { stop -> trade.brokerageAt(stop).pnl }
                        }.first()?.price

                        val rValue = when (stop) {
                            null -> null
                            else -> when (trade.side) {
                                TradeSide.Long -> pnlBD / ((trade.averageEntry - stop) * trade.quantity)
                                TradeSide.Short -> pnlBD / ((stop - trade.averageEntry) * trade.quantity)
                            }.setScale(1, RoundingMode.HALF_EVEN)
                        }

                        Triple(pnlBD, netPnlBD, rValue)
                    }

                    val pnl = tickerStats.sumOf { it.first }
                    val netPnl = tickerStats.sumOf { it.second }
                    val rValue = tickerStats.mapNotNull { it.third }.sumOf { it }

                    val noOfTrades = entries.value.size

                    Model(
                        ticker = entries.key,
                        noOfTrades = noOfTrades.toString(),
                        pnl = pnl.toPlainString(),
                        isProfitable = pnl > BigDecimal.ZERO,
                        netPnl = netPnl.toPlainString(),
                        isNetProfitable = netPnl > BigDecimal.ZERO,
                        fees = (pnl - netPnl).toPlainString(),
                        rValue = "${rValue}R",
                    )
                }.sortedByDescending { it.noOfTrades.toInt() }
        }
    }

    data class Model(
        val ticker: String,
        val noOfTrades: String,
        val pnl: String,
        val isProfitable: Boolean,
        val netPnl: String,
        val isNetProfitable: Boolean,
        val fees: String,
        val rValue: String,
    )

    class Factory(
        private val appPrefs: FlowSettings,
        private val tradingProfiles: TradingProfiles,
    ) : Study.Factory<PNLByTickerStudy> {

        override val name: String = "PNL By Ticker"

        override fun create() = PNLByTickerStudy(appPrefs, tradingProfiles)
    }
}
