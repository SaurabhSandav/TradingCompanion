package com.saurabhsandav.core.ui.stats.studies

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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal class PNLByDayStudy(
    profileId: ProfileId,
    tradingProfiles: TradingProfiles,
) : TableStudy<PNLByDayStudy.Model>() {

    override val schema: TableSchema<Model> = tableSchema {
        addColumnText("Day") { it.day }
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
                .groupBy { it.entryTimestamp.toLocalDateTime(TimeZone.currentSystemDefault()).date }
                .map { (date, tradesByDay) ->

                    val closedTrades = tradesByDay.filter { it.isClosed }
                    val closedTradesIds = closedTrades.map { it.id }

                    tradesRepo.getPrimaryStops(closedTradesIds).map { stops ->

                        val dailyStats = closedTrades.map { trade ->

                            val brokerage = trade.brokerageAtExit()!!
                            val pnlBD = brokerage.pnl
                            val netPnlBD = brokerage.netPNL

                            val stop = stops.find { it.tradeId == trade.id }
                            val rValue = stop?.let { trade.rValueAt(pnl = pnlBD, stop = it) }

                            Triple(pnlBD, netPnlBD, rValue)
                        }

                        val pnl = dailyStats.sumOf { it.first }
                        val netPnl = dailyStats.sumOf { it.second }
                        val rValue = dailyStats.mapNotNull { it.third }.sumOf { it }

                        val day = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(date.toJavaLocalDate())

                        Model(
                            day = day,
                            noOfTrades = tradesByDay.size.toString(),
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
        val day: String,
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
    ) : Study.Factory<PNLByDayStudy> {

        override val name: String = "PNL By Day"

        override fun create() = PNLByDayStudy(profileId, tradingProfiles)
    }
}
