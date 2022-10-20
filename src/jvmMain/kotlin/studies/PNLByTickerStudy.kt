package studies

import AppModule
import androidx.compose.material3.Text
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import model.Side
import ui.common.AppColor
import ui.common.table.TableSchema
import ui.common.table.addColumn
import ui.common.table.addColumnText
import ui.common.table.tableSchema
import utils.brokerage
import java.math.BigDecimal
import java.math.RoundingMode

internal class PNLByTickerStudy(appModule: AppModule) : TableStudy<PNLByTickerStudy.Model>() {

    override val schema: TableSchema<Model> = tableSchema {
        addColumnText("Ticker") { it.ticker }
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
        .getAllClosedTradesDetailed()
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map { getAllClosedTradesDetailed ->

            getAllClosedTradesDetailed
                .groupBy { it.ticker }
                .map { entries ->

                    val stuff = entries.value.map {

                        val entryBD = it.entry.toBigDecimal()
                        val stopBD = it.stop?.toBigDecimalOrNull()
                        val exitBD = it.exit.toBigDecimal()
                        val quantityBD = it.quantity.toBigDecimal()
                        val sideEnum = Side.fromString(it.side)

                        val pnlBD = when (sideEnum) {
                            Side.Long -> (exitBD - entryBD) * quantityBD
                            Side.Short -> (entryBD - exitBD) * quantityBD
                        }

                        val netPnlBD = brokerage(
                            broker = it.broker,
                            instrument = it.instrument,
                            entry = entryBD,
                            exit = exitBD,
                            quantity = quantityBD,
                            side = sideEnum,
                        )

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

                    val noOfTrades = entries.value.size

                    Model(
                        ticker = entries.key,
                        noOfTrades = noOfTrades.toString(),
                        pnl = pnl.toPlainString(),
                        isProfitable = pnl > BigDecimal.ZERO,
                        netPnl = netPnl.toPlainString(),
                        isNetProfitable = netPnl > BigDecimal.ZERO,
                        fees = (pnl - netPnl).toPlainString(),
                        rValue = "${rValue}R",
                    )
                }.sortedByDescending { it.noOfTrades.toInt() }
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

    class Factory(private val appModule: AppModule): Study.Factory<PNLByTickerStudy> {

        override val name: String = "PNL By Ticker"

        override fun create() = PNLByTickerStudy(appModule)
    }
}
