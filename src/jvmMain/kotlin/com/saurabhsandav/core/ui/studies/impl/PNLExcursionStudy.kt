package com.saurabhsandav.core.ui.studies.impl

import androidx.compose.foundation.TooltipArea
import androidx.compose.material3.Text
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.brokerageAt
import com.saurabhsandav.core.trades.brokerageAtExit
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.model.TradeSide
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.Tooltip
import com.saurabhsandav.core.ui.common.table.TableSchema
import com.saurabhsandav.core.ui.common.table.addColumn
import com.saurabhsandav.core.ui.common.table.addColumnText
import com.saurabhsandav.core.ui.common.table.tableSchema
import com.saurabhsandav.core.utils.emitInto
import com.saurabhsandav.core.utils.format
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal class PNLExcursionStudy(
    profileId: ProfileId,
    tradingProfiles: TradingProfiles,
) : TableStudy<PNLExcursionStudy.Model>() {

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
        addColumn(
            header = {

                TooltipArea(
                    tooltip = { Tooltip("Maximum Favorable Excursion") },
                    content = { Text("MFE") },
                )
            },
            content = { Text(it.maxFavorableExcursion) }
        )
        addColumn(
            header = {

                TooltipArea(
                    tooltip = { Tooltip("Maximum Favorable Excursion PNL") },
                    content = { Text("MFE PNL") },
                )
            },
            content = { Text(text = it.mfePNL, color = AppColor.ProfitGreen) }
        )
        addColumn(
            header = {

                TooltipArea(
                    tooltip = { Tooltip("Maximum Adverse Excursion") },
                    content = { Text("MAE") },
                )
            },
            content = { Text(it.maxAdverseExcursion) }
        )
        addColumn(
            header = {

                TooltipArea(
                    tooltip = { Tooltip("Maximum Adverse Excursion PNL") },
                    content = { Text("MAE PNL") },
                )
            },
            content = { Text(text = it.maePNL, color = AppColor.LossRed) }
        )
    }

    override val data: Flow<List<Model>> = flow {

        val tradesRepo = tradingProfiles.getRecord(profileId).trades

        tradesRepo.allTrades.map { trades ->

            trades.filter { it.isClosed }.map { trade ->

                val brokerage = trade.brokerageAtExit()!!
                val pnlBD = brokerage.pnl
                val netPnlBD = brokerage.netPNL

                val entryLDT = trade.entryTimestamp.toLocalDateTime(TimeZone.currentSystemDefault())
                val exitLDT = trade.exitTimestamp?.toLocalDateTime(TimeZone.currentSystemDefault())
                val day = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(entryLDT)

                val stop = tradesRepo.getStopsForTrade(trade.id).map { tradeStops ->
                    tradeStops.maxByOrNull { stop -> trade.brokerageAt(stop).pnl }
                }.first()?.price

                val target = tradesRepo.getTargetsForTrade(trade.id).map { tradeTargets ->
                    tradeTargets.maxByOrNull { target -> trade.brokerageAt(target).pnl }
                }.first()?.price

                val mfeAndMae = tradesRepo.getMfeAndMae(trade.id).first()

                Model(
                    ticker = trade.ticker,
                    quantity = trade.quantity.toPlainString(),
                    side = trade.side.strValue.uppercase(),
                    entry = trade.averageEntry.toPlainString(),
                    stop = stop?.toPlainString() ?: "NA",
                    duration = "$day\n${entryLDT.time} ->\n${exitLDT?.time}",
                    target = target?.toPlainString() ?: "NA",
                    exit = trade.averageExit!!.toPlainString(),
                    pnl = pnlBD.toPlainString(),
                    isProfitable = pnlBD > BigDecimal.ZERO,
                    netPnl = netPnlBD.toPlainString(),
                    isNetProfitable = netPnlBD > BigDecimal.ZERO,
                    maxFavorableExcursion = mfeAndMae?.mfePrice?.toPlainString() ?: "NA",
                    mfePNL = mfeAndMae?.mfePrice?.let { mfePrice ->
                        buildPNLString(
                            side = trade.side,
                            quantity = trade.quantity,
                            entry = trade.averageEntry,
                            stop = stop,
                            exit = mfePrice,
                        )
                    }.orEmpty(),
                    maxAdverseExcursion = mfeAndMae?.maePrice?.toPlainString() ?: "NA",
                    maePNL = mfeAndMae?.maePrice?.let { maePrice ->
                        buildPNLString(
                            side = trade.side,
                            quantity = trade.quantity,
                            entry = trade.averageEntry,
                            stop = stop,
                            exit = maePrice,
                        )
                    }.orEmpty(),
                )
            }
        }.emitInto(this)
    }

    private fun buildPNLString(
        side: TradeSide,
        quantity: BigDecimal,
        entry: BigDecimal,
        stop: BigDecimal?,
        exit: BigDecimal,
    ): String {

        val pnl = when (side) {
            TradeSide.Long -> (exit - entry) * quantity
            TradeSide.Short -> (entry - exit) * quantity
        }

        val rValue = when (stop) {
            null -> null
            else -> when (side) {
                TradeSide.Long -> pnl / ((entry - stop) * quantity)
                TradeSide.Short -> pnl / ((stop - entry) * quantity)
            }.setScale(1, RoundingMode.HALF_EVEN).toPlainString()
        }

        return pnl.toPlainString() + rValue?.let { " (${it}R)" }.orEmpty()
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
        val maxFavorableExcursion: String,
        val mfePNL: String,
        val maxAdverseExcursion: String,
        val maePNL: String,
    )

    class Factory(
        private val profileId: ProfileId,
        private val tradingProfiles: TradingProfiles,
    ) : Study.Factory<PNLExcursionStudy> {

        override val name: String = "PNL Excursion"

        override fun create() = PNLExcursionStudy(profileId, tradingProfiles)
    }
}
