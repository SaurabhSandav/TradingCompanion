package com.saurabhsandav.core.ui.studies.impl

import androidx.compose.material3.Text
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trades.model.TradeSide
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.table.TableSchema
import com.saurabhsandav.core.ui.common.table.addColumn
import com.saurabhsandav.core.ui.common.table.addColumnText
import com.saurabhsandav.core.ui.common.table.tableSchema
import com.saurabhsandav.core.utils.brokerage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.datetime.toJavaLocalDateTime
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal class PNLStudy(appModule: AppModule) : TableStudy<PNLStudy.Model>() {

    private val tradesRepo = appModule.tradingProfiles.currentProfile.map { profile ->
        appModule.tradingProfiles.getRecord(profile.id).trades
    }

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

    override val data: Flow<List<Model>> = tradesRepo.flatMapLatest { tradesRepo ->

        tradesRepo.allTrades.map { trades ->

            trades.filter { it.isClosed }.map { trade ->

                val brokerage = brokerage(
                    broker = trade.broker,
                    instrument = trade.instrument,
                    entry = trade.averageEntry,
                    exit = trade.averageExit!!,
                    quantity = trade.quantity,
                    side = trade.side,
                )

                val pnlBD = brokerage.pnl
                val netPnlBD = brokerage.netPNL

                val stop = tradesRepo.getStopsForTrade(trade.id).map { tradeStops ->

                    tradeStops.maxByOrNull { tradeStop ->

                        brokerage(
                            broker = trade.broker,
                            instrument = trade.instrument,
                            entry = trade.averageEntry,
                            exit = tradeStop.price,
                            quantity = trade.quantity,
                            side = trade.side,
                        ).pnl
                    }
                }.first()?.price

                val rValue = when (stop) {
                    null -> null
                    else -> when (trade.side) {
                        TradeSide.Long -> pnlBD / ((trade.averageEntry - stop) * trade.quantity)
                        TradeSide.Short -> pnlBD / ((stop - trade.averageEntry) * trade.quantity)
                    }.setScale(1, RoundingMode.HALF_EVEN)
                }

                val day = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
                    .format(trade.entryTimestamp.toJavaLocalDateTime())

                val target = tradesRepo.getTargetsForTrade(trade.id).map { tradeTargets ->

                    tradeTargets.maxByOrNull { tradeTarget ->

                        brokerage(
                            broker = trade.broker,
                            instrument = trade.instrument,
                            entry = trade.averageEntry,
                            exit = tradeTarget.price,
                            quantity = trade.quantity,
                            side = trade.side,
                        ).pnl
                    }
                }.first()?.price

                Model(
                    ticker = trade.ticker,
                    quantity = trade.quantity.toPlainString(),
                    side = trade.side.strValue.uppercase(),
                    entry = trade.averageEntry.toPlainString(),
                    stop = stop?.toPlainString() ?: "NA",
                    duration = "$day\n${trade.entryTimestamp.time} ->\n${trade.exitTimestamp?.time}",
                    target = target?.toPlainString() ?: "NA",
                    exit = trade.averageExit.toPlainString(),
                    pnl = pnlBD.toPlainString(),
                    isProfitable = pnlBD > BigDecimal.ZERO,
                    netPnl = netPnlBD.toPlainString(),
                    isNetProfitable = netPnlBD > BigDecimal.ZERO,
                    fees = (pnlBD - netPnlBD).toPlainString(),
                    rValue = rValue?.let { "${it}R" }.orEmpty(),
                )
            }
        }
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

    class Factory(private val appModule: AppModule) : Study.Factory<PNLStudy> {

        override val name: String = "PNL"

        override fun create() = PNLStudy(appModule)
    }
}
