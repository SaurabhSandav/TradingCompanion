package com.saurabhsandav.core.ui.studies.impl

import androidx.compose.material3.Text
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.brokerageAtExit
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.rValueAt
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.table.TableSchema
import com.saurabhsandav.core.ui.common.table.addColumn
import com.saurabhsandav.core.ui.common.table.addColumnText
import com.saurabhsandav.core.ui.common.table.tableSchema
import com.saurabhsandav.core.utils.emitInto
import kotlinx.coroutines.flow.*
import java.math.BigDecimal

internal class PNLByTickerStudy(
    profileId: ProfileId,
    tradingProfiles: TradingProfiles,
) : TableStudy<PNLByTickerStudy.Model>() {

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

    override val data: Flow<List<Model>> = flow {

        val tradesRepo = tradingProfiles.getRecord(profileId).trades

        tradesRepo.allTrades.flatMapLatest { trades ->

            trades
                .groupBy { it.ticker }
                .map { (ticker, tradesByTicker) ->

                    val closedTrades = tradesByTicker.filter { it.isClosed }
                    val closedTradesIds = closedTrades.map { it.id }

                    tradesRepo.getPrimaryStops(closedTradesIds).map { stops ->

                        val tickerStats = closedTrades.filter { it.isClosed }.map { trade ->

                            val brokerage = trade.brokerageAtExit()!!
                            val pnlBD = brokerage.pnl
                            val netPnlBD = brokerage.netPNL

                            val stop = stops.find { it.tradeId == trade.id }
                            val rValue = stop?.let { trade.rValueAt(pnl = pnlBD, stop = it) }

                            Triple(pnlBD, netPnlBD, rValue)
                        }

                        val pnl = tickerStats.sumOf { it.first }
                        val netPnl = tickerStats.sumOf { it.second }
                        val rValue = tickerStats.mapNotNull { it.third }.sumOf { it }

                        Model(
                            ticker = ticker,
                            noOfTrades = tradesByTicker.size.toString(),
                            pnl = pnl.toPlainString(),
                            isProfitable = pnl > BigDecimal.ZERO,
                            netPnl = netPnl.toPlainString(),
                            isNetProfitable = netPnl > BigDecimal.ZERO,
                            fees = (pnl - netPnl).toPlainString(),
                            rValue = "${rValue}R",
                        )
                    }
                }.let { flows ->
                    combine(flows) { models -> models.asList().sortedByDescending { it.noOfTrades.toInt() } }
                }
        }.emitInto(this)
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
        private val profileId: ProfileId,
        private val tradingProfiles: TradingProfiles,
    ) : Study.Factory<PNLByTickerStudy> {

        override val name: String = "PNL By Ticker"

        override fun create() = PNLByTickerStudy(profileId, tradingProfiles)
    }
}
