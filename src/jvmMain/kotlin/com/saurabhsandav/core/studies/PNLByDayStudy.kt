package com.saurabhsandav.core.studies

import androidx.compose.material3.Text
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.model.Side
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.table.TableSchema
import com.saurabhsandav.core.ui.common.table.addColumn
import com.saurabhsandav.core.ui.common.table.addColumnText
import com.saurabhsandav.core.ui.common.table.tableSchema
import com.saurabhsandav.core.utils.brokerage
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDate
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal class PNLByDayStudy(appModule: AppModule) : TableStudy<PNLByDayStudy.Model>() {

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

    override val data: Flow<List<Model>> = appModule.appDB
        .closedTradeQueries
        .getAll()
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map { closedTrades ->

            closedTrades
                .groupBy { LocalDateTime.parse(it.entryDate).date }
                .map { entries ->

                    val stuff = entries.value.map {

                        val entryBD = it.entry.toBigDecimal()
                        val stopBD = it.stop?.toBigDecimalOrNull()
                        val exitBD = it.exit.toBigDecimal()
                        val quantityBD = it.quantity.toBigDecimal()
                        val sideEnum = Side.fromString(it.side)

                        val brokerage = brokerage(
                            broker = it.broker,
                            instrument = it.instrument,
                            entry = entryBD,
                            exit = exitBD,
                            quantity = quantityBD,
                            side = sideEnum,
                        )

                        val pnlBD = brokerage.pnl
                        val netPnlBD = brokerage.netPNL

                        val rValue = when (stopBD) {
                            null -> null
                            else -> when (sideEnum) {
                                Side.Long -> pnlBD / ((entryBD - stopBD) * quantityBD)
                                Side.Short -> pnlBD / ((stopBD - entryBD) * quantityBD)
                            }.setScale(1, RoundingMode.HALF_EVEN)
                        }

                        Triple(pnlBD, netPnlBD, rValue)
                    }

                    val pnl = stuff.sumOf { it.first }
                    val netPnl = stuff.sumOf { it.second }
                    val rValue = stuff.mapNotNull { it.third }.sumOf { it }

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

    class Factory(private val appModule: AppModule): Study.Factory<PNLByDayStudy> {

        override val name: String = "PNL By Day"

        override fun create() = PNLByDayStudy(appModule)
    }
}
