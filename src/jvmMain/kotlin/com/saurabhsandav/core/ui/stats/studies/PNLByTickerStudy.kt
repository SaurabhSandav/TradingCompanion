package com.saurabhsandav.core.ui.stats.studies

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.brokerageAtExit
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.rValueAt
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.table2.*
import com.saurabhsandav.core.utils.emitInto
import kotlinx.coroutines.flow.*
import java.math.BigDecimal

internal class PNLByTickerStudy(
    profileId: ProfileId,
    tradingProfiles: TradingProfiles,
) : Study {

    @Composable
    override fun render() {

        val items by data.collectAsState(emptyList())

        LazyTable(
            modifier = Modifier.fillMaxSize(),
            headerContent = {

                Schema.SimpleHeader {
                    ticker.text { "Ticker" }
                    trades.text { "Trades" }
                    pnl.text { "PNL" }
                    netPnl.text { "Net PNL" }
                    fees.text { "Fees" }
                    rValue.text { "R" }
                }
            },
        ) {

            items(
                items = items,
            ) { item ->

                Column {

                    Schema.SimpleRow {
                        ticker.text { item.ticker }
                        trades.text { item.noOfTrades }
                        pnl {
                            Text(
                                text = item.pnl,
                                color = if (item.isProfitable) AppColor.ProfitGreen else AppColor.LossRed,
                            )
                        }
                        netPnl {
                            Text(
                                text = item.netPnl,
                                color = if (item.isNetProfitable) AppColor.ProfitGreen else AppColor.LossRed,
                            )
                        }
                        fees.text { item.fees }
                        rValue {
                            Text(
                                text = item.rValue,
                                color = if (item.isProfitable) AppColor.ProfitGreen else AppColor.LossRed,
                            )
                        }
                    }

                    HorizontalDivider()
                }
            }
        }
    }

    private val data: Flow<List<Model>> = flow {

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

    private object Schema : TableSchema() {

        val ticker = cell()
        val trades = cell()
        val pnl = cell()
        val netPnl = cell()
        val fees = cell()
        val rValue = cell()
    }

    class Factory(
        private val profileId: ProfileId,
        private val tradingProfiles: TradingProfiles,
    ) : Study.Factory<PNLByTickerStudy> {

        override val name: String = "PNL By Ticker"

        override fun create() = PNLByTickerStudy(profileId, tradingProfiles)
    }
}
