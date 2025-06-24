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
import com.saurabhsandav.core.trading.record.TradingProfiles
import com.saurabhsandav.core.trading.record.brokerageAtExit
import com.saurabhsandav.core.trading.record.model.ProfileId
import com.saurabhsandav.core.trading.record.rValueAt
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.table.LazyTable
import com.saurabhsandav.core.ui.common.table.SimpleHeader
import com.saurabhsandav.core.ui.common.table.SimpleRow
import com.saurabhsandav.core.ui.common.table.TableSchema
import com.saurabhsandav.core.ui.common.table.content
import com.saurabhsandav.core.ui.common.table.text
import com.saurabhsandav.core.utils.emitInto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal

internal class PNLByMonthStudy(
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
                    month.text { "Month" }
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
                        month.text { item.month }
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

        tradingRecord.trades.allTrades.flatMapLatest { allTrades ->

            allTrades
                .groupBy { trade ->
                    val ldt = trade.entryTimestamp.toLocalDateTime(TimeZone.currentSystemDefault())
                    "${ldt.month} ${ldt.year}"
                }
                .map { (month, tradesByMonth) ->

                    val closedTrades = tradesByMonth.filter { it.isClosed }
                    val closedTradesIds = closedTrades.map { it.id }

                    tradingRecord.stops.getPrimary(closedTradesIds).map { stops ->

                        val monthlyStats = closedTrades.map { trade ->

                            val brokerage = trade.brokerageAtExit()!!
                            val pnlBD = brokerage.pnl
                            val netPnlBD = brokerage.netPNL

                            val stop = stops.find { it.tradeId == trade.id }
                            val rValue = stop?.let { trade.rValueAt(pnl = pnlBD, stop = it) }

                            Triple(pnlBD, netPnlBD, rValue)
                        }

                        val pnl = monthlyStats.sumOf { it.first }
                        val netPnl = monthlyStats.sumOf { it.second }
                        val rValue = monthlyStats.mapNotNull { it.third }.sumOf { it }

                        Model(
                            month = month,
                            noOfTrades = tradesByMonth.size.toString(),
                            pnl = pnl.toPlainString(),
                            isProfitable = pnl > BigDecimal.ZERO,
                            netPnl = netPnl.toPlainString(),
                            isNetProfitable = netPnl > BigDecimal.ZERO,
                            fees = (pnl - netPnl).toPlainString(),
                            rValue = "${rValue}R",
                        )
                    }
                }.let { flows -> combine(flows) { it.asList() } }
        }.emitInto(this)
    }

    data class Model(
        val month: String,
        val noOfTrades: String,
        val pnl: String,
        val isProfitable: Boolean,
        val netPnl: String,
        val isNetProfitable: Boolean,
        val fees: String,
        val rValue: String,
    )

    private object Schema : TableSchema() {

        val month = cell()
        val trades = cell()
        val pnl = cell()
        val netPnl = cell()
        val fees = cell()
        val rValue = cell()
    }

    class Factory(
        private val profileId: ProfileId,
        private val tradingProfiles: TradingProfiles,
    ) : Study.Factory<PNLByMonthStudy> {

        override val name: String = "PNL By Month"

        override fun create() = PNLByMonthStudy(profileId, tradingProfiles)
    }
}
