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
import com.saurabhsandav.core.trading.ProfileId
import com.saurabhsandav.core.trading.TradingProfiles
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.getSymbolTitle
import com.saurabhsandav.core.ui.common.table.LazyTable
import com.saurabhsandav.core.ui.common.table.SimpleHeader
import com.saurabhsandav.core.ui.common.table.SimpleRow
import com.saurabhsandav.core.ui.common.table.TableSchema
import com.saurabhsandav.core.ui.common.table.content
import com.saurabhsandav.core.ui.common.table.text
import com.saurabhsandav.core.ui.stats.StatsGraph
import com.saurabhsandav.core.utils.emitInto
import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.kbigdecimal.sumOf
import com.saurabhsandav.trading.record.brokerageAtExit
import com.saurabhsandav.trading.record.rValueAt
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

internal class PNLBySymbolStudy(
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
                    symbol.text { "Symbol" }
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

                Column(Modifier.animateItem()) {

                    Schema.SimpleRow {
                        symbol.text { item.ticker }
                        trades.text { item.noOfTrades }
                        pnl.content {
                            Text(
                                text = item.pnl,
                                color = if (item.isProfitable) AppColor.ProfitGreen else AppColor.LossRed,
                            )
                        }
                        netPnl.content {
                            Text(
                                text = item.netPnl,
                                color = if (item.isNetProfitable) AppColor.ProfitGreen else AppColor.LossRed,
                            )
                        }
                        fees.text { item.fees }
                        rValue.content {
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

        val tradingRecord = tradingProfiles.getRecord(profileId)

        tradingRecord.trades.allTradesDisplay.flatMapLatest { allTrades ->

            allTrades
                .groupBy { it.symbolId }
                .map { (symbolId, tradesBySymbol) ->

                    val closedTrades = tradesBySymbol.filter { it.isClosed }
                    val closedTradesIds = closedTrades.map { it.id }

                    tradingRecord.stops.getPrimary(closedTradesIds).map { stops ->

                        val symbolStats = closedTrades.filter { it.isClosed }.map { trade ->

                            val broker = tradingRecord.brokerProvider.getBroker(trade.brokerId)
                            val brokerage = trade.brokerageAtExit(broker)!!
                            val pnlBD = brokerage.pnl
                            val netPnlBD = brokerage.netPNL

                            val stop = stops.find { it.tradeId == trade.id }
                            val rValue = stop?.let { trade.rValueAt(pnl = pnlBD, stop = it) }

                            Triple(pnlBD, netPnlBD, rValue)
                        }

                        val pnl = symbolStats.sumOf { it.first }
                        val netPnl = symbolStats.sumOf { it.second }
                        val rValue = symbolStats.mapNotNull { it.third }.sumOf { it }

                        Model(
                            ticker = tradesBySymbol.first().getSymbolTitle(),
                            noOfTrades = tradesBySymbol.size.toString(),
                            pnl = pnl.toString(),
                            isProfitable = pnl > KBigDecimal.Zero,
                            netPnl = netPnl.toString(),
                            isNetProfitable = netPnl > KBigDecimal.Zero,
                            fees = (pnl - netPnl).toString(),
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

        val symbol = cell()
        val trades = cell()
        val pnl = cell()
        val netPnl = cell()
        val fees = cell()
        val rValue = cell()
    }

    @ContributesIntoSet(StatsGraph::class, binding<Study.Factory<out Study>>())
    @Inject
    class Factory(
        private val profileId: ProfileId,
        private val tradingProfiles: TradingProfiles,
    ) : Study.Factory<PNLBySymbolStudy> {

        override val name: String = "PNL By Symbol"

        override fun create() = PNLBySymbolStudy(profileId, tradingProfiles)
    }
}
