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
import com.saurabhsandav.core.utils.getCurrentTradingRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.datetime.toJavaLocalDate
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal class PNLByDayStudy(appModule: AppModule) : TableStudy<PNLByDayStudy.Model>() {

    private val tradesRepo = appModule.appPrefs
        .getCurrentTradingRecord(appModule.tradingProfiles)
        .map { record -> record.trades }

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

    override val data: Flow<List<Model>> = tradesRepo.flatMapLatest { tradesRepo ->

        tradesRepo.allTrades.map { trades ->

            trades
                .groupBy { it.entryTimestamp.date }
                .map { entries ->

                    val dailyStats = entries.value.filter { it.isClosed }.map { trade ->

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

                        Triple(pnlBD, netPnlBD, rValue)
                    }

                    val pnl = dailyStats.sumOf { it.first }
                    val netPnl = dailyStats.sumOf { it.second }
                    val rValue = dailyStats.mapNotNull { it.third }.sumOf { it }

                    val day = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(entries.key.toJavaLocalDate())

                    val noOfTrades = entries.value.size

                    Model(
                        day = day,
                        noOfTrades = noOfTrades.toString(),
                        pnl = pnl.toPlainString(),
                        isProfitable = pnl > BigDecimal.ZERO,
                        netPnl = netPnl.toPlainString(),
                        isNetProfitable = netPnl > BigDecimal.ZERO,
                        fees = (pnl - netPnl).toPlainString(),
                        rValue = "${rValue}R",
                    )
                }
        }
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

    class Factory(private val appModule: AppModule) : Study.Factory<PNLByDayStudy> {

        override val name: String = "PNL By Day"

        override fun create() = PNLByDayStudy(appModule)
    }
}
