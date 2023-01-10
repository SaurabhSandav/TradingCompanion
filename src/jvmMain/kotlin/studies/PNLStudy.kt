package studies

import AppModule
import androidx.compose.material3.Text
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import model.Side
import ui.common.AppColor
import ui.common.table.TableSchema
import ui.common.table.addColumn
import ui.common.table.addColumnText
import ui.common.table.tableSchema
import utils.brokerage
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal class PNLStudy(appModule: AppModule) : TableStudy<PNLStudy.Model>() {

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

    override val data: Flow<List<Model>> = appModule.appDB
        .closedTradeQueries
        .getAll { _, broker, ticker, instrument, quantity, _, side, entry, stop, entryDate, target, exit, exitDate ->

            val entryBD = entry.toBigDecimal()
            val stopBD = stop?.toBigDecimalOrNull()
            val exitBD = exit.toBigDecimal()
            val quantityBD = quantity.toBigDecimal()
            val sideEnum = Side.fromString(side)

            val brokerage = brokerage(
                broker = broker,
                instrument = instrument,
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
                }.setScale(1, RoundingMode.HALF_EVEN).toPlainString()
            }

            val day = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
                .format(entryDate.toLocalDateTime().toJavaLocalDateTime())

            Model(
                ticker = ticker,
                quantity = quantity,
                side = side.uppercase(),
                entry = entry,
                stop = stop ?: "NA",
                duration = "$day\n${entryDate.toLocalDateTime().time} ->\n${exitDate.toLocalDateTime().time}",
                target = target ?: "NA",
                exit = exit,
                pnl = pnlBD.toPlainString(),
                isProfitable = pnlBD > BigDecimal.ZERO,
                netPnl = netPnlBD.toPlainString(),
                isNetProfitable = netPnlBD > BigDecimal.ZERO,
                fees = (pnlBD - netPnlBD).toPlainString(),
                rValue = rValue?.let { "${it}R" }.orEmpty(),
            )
        }
        .asFlow()
        .mapToList(Dispatchers.IO)

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

    class Factory(private val appModule: AppModule): Study.Factory<PNLStudy> {

        override val name: String = "PNL"

        override fun create() = PNLStudy(appModule)
    }
}
