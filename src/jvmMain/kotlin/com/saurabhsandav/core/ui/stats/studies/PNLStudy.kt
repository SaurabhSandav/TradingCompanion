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
import com.saurabhsandav.core.utils.format
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal class PNLStudy(
    profileId: ProfileId,
    tradingProfiles: TradingProfiles,
) : TableStudy<PNLStudy.Model>() {

    override val schema: TableSchema<Model> = tableSchema {
        addColumnText("Ticker") { it.ticker }
        addColumnText("Quantity") { it.quantity }
        addColumn("Side") {
            Text(it.side, color = if (it.side == "LONG") AppColor.ProfitGreen else AppColor.LossRed)
        }
        addColumnText("Entry") { it.entry }
        addColumnText("Stop") { it.stop }
        addColumnText("Duration") { it.duration }
        addColumnText("Target") { it.target }
        addColumnText("Exit") { it.exit }
        addColumn("PNL") {
            Text(it.pnl, color = if (it.isProfitable) AppColor.ProfitGreen else AppColor.LossRed)
        }
        addColumn("Net PNL") {
            Text(it.netPnl, color = if (it.isNetProfitable) AppColor.ProfitGreen else AppColor.LossRed)
        }
        addColumnText("Fees") { it.fees }
        addColumnText("R") { it.rValue }
    }

    override val data: Flow<List<Model>> = flow {

        val tradesRepo = tradingProfiles.getRecord(profileId).trades

        tradesRepo.allTrades.flatMapLatest { trades ->

            val closedTrades = trades.filter { it.isClosed }
            val closedTradesIds = closedTrades.map { it.id }

            combine(
                tradesRepo.getPrimaryStops(closedTradesIds),
                tradesRepo.getPrimaryTargets(closedTradesIds),
            ) { stops, targets ->

                closedTrades.map { trade ->

                    val stop = stops.find { it.tradeId == trade.id }
                    val target = targets.find { it.tradeId == trade.id }

                    val brokerage = trade.brokerageAtExit()!!
                    val pnlBD = brokerage.pnl
                    val netPnlBD = brokerage.netPNL

                    val rValue = stop?.let { trade.rValueAt(pnl = pnlBD, stop = it) }

                    val entryLDT = trade.entryTimestamp.toLocalDateTime(TimeZone.currentSystemDefault())
                    val exitLDT = trade.exitTimestamp?.toLocalDateTime(TimeZone.currentSystemDefault())
                    val day = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(entryLDT)

                    Model(
                        ticker = trade.ticker,
                        quantity = trade.quantity.toPlainString(),
                        side = trade.side.strValue.uppercase(),
                        entry = trade.averageEntry.toPlainString(),
                        stop = stop?.price?.toPlainString() ?: "NA",
                        duration = "$day\n${entryLDT.time} ->\n${exitLDT?.time}",
                        target = target?.price?.toPlainString() ?: "NA",
                        exit = trade.averageExit!!.toPlainString(),
                        pnl = pnlBD.toPlainString(),
                        isProfitable = pnlBD > BigDecimal.ZERO,
                        netPnl = netPnlBD.toPlainString(),
                        isNetProfitable = netPnlBD > BigDecimal.ZERO,
                        fees = (pnlBD - netPnlBD).toPlainString(),
                        rValue = rValue?.let { "${it}R" }.orEmpty(),
                    )
                }
            }
        }.emitInto(this)
    }

    data class Model(
        val ticker: String,
        val quantity: String,
        val side: String,
        val entry: String,
        val stop: String,
        val duration: String,
        val target: String,
        val exit: String,
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
    ) : Study.Factory<PNLStudy> {

        override val name: String = "PNL"

        override fun create() = PNLStudy(profileId, tradingProfiles)
    }
}
