package closedtrades

import AppModule
import Side
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.GetAllClosedTradesDetailed
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import utils.brokerage
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

internal class ClosedTradesPresenter(
    private val coroutineScope: CoroutineScope,
    private val appModule: AppModule,
) {

    val state = coroutineScope.launchMolecule(RecompositionClock.Immediate) {

        val closedTradesEntries by remember {
            appModule.appDB.closedTradeQueries
                .getAllClosedTradesDetailed()
                .asFlow()
                .mapToList(Dispatchers.IO)
                .map { getAllClosedTradesDetailed ->

                    getAllClosedTradesDetailed
                        .groupBy { LocalDateTime.parse(it.entryDate).date }
                        .mapKeys { (date, _) -> ClosedTradeListItem.DayHeader(date.toString()) }
                        .mapValues { (_, list) -> list.map { it.toClosedTradeListEntry() } }
                }
        }.collectAsState(emptyMap())

        return@launchMolecule ClosedTradesState(
            closedTradesItems = closedTradesEntries,
        )
    }

    private fun GetAllClosedTradesDetailed.toClosedTradeListEntry(): ClosedTradeListItem.Entry {

        val instrumentCapitalized = instrument
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val entryBD = entry.toBigDecimal()
        val stopBD = stop?.toBigDecimal()
        val exitBD = exit.toBigDecimal()
        val quantityBD = quantity.toBigDecimal()
        val side = Side.fromString(side)

        val pnlBD = when (side) {
            Side.Long -> (exitBD - entryBD) * quantityBD
            Side.Short -> (entryBD - exitBD) * quantityBD
        }

        val netPnlBD = brokerage(
            broker = broker,
            instrument = instrument,
            entry = entryBD,
            exit = exitBD,
            quantity = quantityBD,
            side = side,
        )

        val rValue = when (stopBD) {
            null -> "NA"
            else -> when (side) {
                Side.Long -> pnlBD / ((entryBD - stopBD) * quantityBD)
                Side.Short -> pnlBD / ((stopBD - entryBD) * quantityBD)
            }.setScale(1, RoundingMode.HALF_EVEN).toPlainString()
        }

        val timeZone = TimeZone.of("Asia/Kolkata")
        val entryDateTime = LocalDateTime.parse(entryDate)
        val entryInstant = entryDateTime.toInstant(timeZone)
        val exitDateTime = LocalDateTime.parse(exitDate)
        val exitInstant = exitDateTime.toInstant(timeZone)
        val s = (exitInstant - entryInstant).inWholeSeconds

        val duration = "%02d:%02d:%02d".format(s / 3600, (s % 3600) / 60, (s % 60))

        return ClosedTradeListItem.Entry(
            id = id,
            broker = "$broker ($instrumentCapitalized)",
            ticker = ticker,
            quantity = lots?.let { "$quantity ($it ${if (it == 1) "lot" else "lots"})" } ?: quantity,
            side = this@toClosedTradeListEntry.side
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
            entry = entry,
            stop = stop ?: "NA",
            entryTime = entryDateTime.time.toString(),
            target = target ?: "NA",
            exit = exit,
            exitTime = exitDateTime.time.toString(),
            pnl = pnlBD.toPlainString() + " (${rValue}R)",
            netPnl = netPnlBD.toPlainString(),
            fees = (pnlBD - netPnlBD).toPlainString(),
            duration = duration,
            isProfitable = netPnlBD > BigDecimal.ZERO,
            maxFavorableExcursion = maxFavorableExcursion,
            maxAdverseExcursion = maxAdverseExcursion,
            persisted = persisted.toBoolean(),
            persistenceResult = persistenceResult,
        )
    }
}
