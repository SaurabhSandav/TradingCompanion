package com.saurabhsandav.core.studies

import androidx.compose.foundation.TooltipArea
import androidx.compose.material3.Text
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.model.Side
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.Tooltip
import com.saurabhsandav.core.ui.common.table.TableSchema
import com.saurabhsandav.core.ui.common.table.addColumn
import com.saurabhsandav.core.ui.common.table.addColumnText
import com.saurabhsandav.core.ui.common.table.tableSchema
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal class PNLExcursionStudy(appModule: AppModule) : TableStudy<PNLExcursionStudy.Model>() {

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

    override val data: Flow<List<Model>> = appModule.appDB.closedTradeQueries.getAllClosedTradesDetailed {
            _, _, ticker, _, quantity, _, side, entry, stop, entryDate, target,
            exit, exitDate, maxFavorableExcursion, maxAdverseExcursion, _, _, _,
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

    class Factory(private val appModule: AppModule): Study.Factory<PNLExcursionStudy> {

        override val name: String = "PNL Excursion"

        override fun create() = PNLExcursionStudy(appModule)
    }
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
