package com.saurabhsandav.core.ui.studies.impl

import androidx.compose.foundation.TooltipArea
import androidx.compose.material3.Text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.saurabhsandav.core.trades.*
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.Tooltip
import com.saurabhsandav.core.ui.common.table.TableSchema
import com.saurabhsandav.core.ui.common.table.addColumn
import com.saurabhsandav.core.ui.common.table.addColumnText
import com.saurabhsandav.core.ui.common.table.tableSchema
import com.saurabhsandav.core.utils.emitInto
import com.saurabhsandav.core.utils.format
import kotlinx.coroutines.flow.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
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
        addColumnText("Target") { it.target }
        addColumnText("Duration") { it.duration }
        addColumnText("Exit") { it.exit }
        addColumn("PNL") {
            Text(it.pnl, color = if (it.isProfitable) AppColor.ProfitGreen else AppColor.LossRed)
        }
        addColumn(
            header = {

                TooltipArea(
                    tooltip = { Tooltip("Excursions In Trade") },
                    content = { Text("In Trade") },
                )
            },
            content = { Text(it.inTrade) }
        )
        addColumn(
            header = {

                TooltipArea(
                    tooltip = { Tooltip("Excursions In Session") },
                    content = { Text("In Session") },
                )
            },
            content = { Text(it.inSession) }
        )
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

                    val brokerage = trade.brokerageAtExit()!!
                    val pnlBD = brokerage.pnl

                    val entryLDT = trade.entryTimestamp.toLocalDateTime(TimeZone.currentSystemDefault())
                    val exitLDT = trade.exitTimestamp?.toLocalDateTime(TimeZone.currentSystemDefault())
                    val day = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(entryLDT)

                    val stop = stops.find { it.tradeId == trade.id }
                    val target = targets.find { it.tradeId == trade.id }?.price

                    val rValue = stop?.let { trade.rValueAt(pnl = trade.pnl, stop = it) }
                    val rValueStr = rValue?.let { " | ${it.toPlainString()}R" }.orEmpty()

                    val excursions = tradesRepo.getExcursions(trade.id).first()

                    Model(
                        ticker = trade.ticker,
                        quantity = trade.quantity.toPlainString(),
                        side = trade.side.strValue.uppercase(),
                        entry = trade.averageEntry.toPlainString(),
                        stop = stop?.price?.toPlainString() ?: "NA",
                        duration = "$day\n${entryLDT.time} ->\n${exitLDT?.time}",
                        target = target?.toPlainString() ?: "NA",
                        exit = trade.averageExit!!.toPlainString(),
                        pnl = "${pnlBD.toPlainString()}${rValueStr}",
                        isProfitable = pnlBD > BigDecimal.ZERO,
                        inTrade = trade.buildExcursionString(stop, excursions, true),
                        inSession = trade.buildExcursionString(stop, excursions, false),
                    )
                }
            }
        }.emitInto(this)
    }

    private fun Trade.buildExcursionString(
        stop: TradeStop?,
        excursions: TradeExcursions?,
        inTrade: Boolean,
    ): AnnotatedString {

        excursions ?: return AnnotatedString("NA")

        val maePrice = when {
            inTrade -> excursions.tradeMaePrice
            else -> excursions.sessionMaePrice
        }

        val mfePrice = when {
            inTrade -> excursions.tradeMfePrice
            else -> excursions.sessionMfePrice
        }

        val maePnl = when {
            inTrade -> excursions.tradeMaePnl
            else -> excursions.sessionMaePnl
        }

        val mfePnl = when {
            inTrade -> excursions.tradeMfePnl
            else -> excursions.sessionMfePnl
        }

        val maeRStr = when {
            inTrade -> getRString(excursions.tradeMaePnl, stop)
            else -> getRString(excursions.sessionMaePnl, stop)
        }

        val mfeRStr = when {
            inTrade -> getRString(excursions.tradeMfePnl, stop)
            else -> getRString(excursions.sessionMfePnl, stop)
        }

        return buildAnnotatedString {
            withStyle(SpanStyle(color = AppColor.LossRed)) {
                appendLine("MAE: $maePrice | $maePnl$maeRStr")
            }
            withStyle(SpanStyle(color = AppColor.ProfitGreen)) {
                appendLine("MFE: $mfePrice | $mfePnl$mfeRStr")
            }
        }
    }

    private fun Trade.getRString(
        pnl: BigDecimal,
        stop: TradeStop?,
    ): String {

        stop ?: return ""

        val rValueStr = rValueAt(
            stop = stop,
            pnl = pnl,
        ).toString()

        return " | ${rValueStr}R"
    }

    data class Model(
        val ticker: String,
        val quantity: String,
        val side: String,
        val entry: String,
        val stop: String,
        val target: String,
        val duration: String,
        val exit: String,
        val pnl: String,
        val isProfitable: Boolean,
        val inTrade: AnnotatedString,
        val inSession: AnnotatedString,
    )

    class Factory(
        private val profileId: ProfileId,
        private val tradingProfiles: TradingProfiles,
    ) : Study.Factory<PNLExcursionStudy> {

        override val name: String = "PNL Excursion"

        override fun create() = PNLExcursionStudy(profileId, tradingProfiles)
    }
}
