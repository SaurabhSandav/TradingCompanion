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
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal class PNLExcursionStudy(appModule: AppModule) : TableStudy<PNLExcursionStudy.Model>() {

    override val name: String = "PNL Excursion"

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
        addColumnText("PNL") { it.pnl }
        addColumnText("MFE") { it.maxFavorableExcursion }
        addColumn("MFE PNL") {
            Text(text = it.mfePNL, color = AppColor.ProfitGreen)
        }
        addColumnText("MAE") { it.maxAdverseExcursion }
        addColumn("MAE PNL") {
            Text(text = it.maePNL, color = AppColor.LossRed)
        }
    }

    override val data: Flow<List<Model>> = appModule.appDB.closedTradeQueries.getAllClosedTradesDetailed {
            _, _, ticker, _, quantity, _, side, entry, stop, entryDate, target,
            exit, exitDate, maxFavorableExcursion, maxAdverseExcursion, _, _,
        ->

        val entryBD = entry.toBigDecimal()
        val stopBD = stop?.toBigDecimalOrNull()
        val exitBD = exit.toBigDecimal()
        val quantityBD = quantity.toBigDecimal()
        val sideEnum = Side.fromString(side)

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
            pnl = buildPNLString(sideEnum, quantityBD, entryBD, stopBD, exitBD),
            maxFavorableExcursion = maxFavorableExcursion.orEmpty(),
            mfePNL = maxFavorableExcursion?.let {
                buildPNLString(sideEnum, quantityBD, entryBD, stopBD, it.toBigDecimal())
            }.orEmpty(),
            maxAdverseExcursion = maxAdverseExcursion.orEmpty(),
            maePNL = maxAdverseExcursion?.let {
                buildPNLString(sideEnum, quantityBD, entryBD, stopBD, it.toBigDecimal())
            }.orEmpty(),
        )
    }.asFlow().mapToList(Dispatchers.IO)

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
        val maxFavorableExcursion: String,
        val mfePNL: String,
        val maxAdverseExcursion: String,
        val maePNL: String,
    )
}

private fun buildPNLString(
    side: Side,
    quantity: BigDecimal,
    entry: BigDecimal,
    stop: BigDecimal?,
    exit: BigDecimal,
): String {

    val pnl = when (side) {
        Side.Long -> (exit - entry) * quantity
        Side.Short -> (entry - exit) * quantity
    }

    val rValue = when (stop) {
        null -> null
        else -> when (side) {
            Side.Long -> pnl / ((entry - stop) * quantity)
            Side.Short -> pnl / ((stop - entry) * quantity)
        }.setScale(1, RoundingMode.HALF_EVEN).toPlainString()
    }

    return pnl.toPlainString() + rValue?.let { " (${it}R)" }.orEmpty()
}
