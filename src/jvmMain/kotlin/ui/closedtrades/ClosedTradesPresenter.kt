package ui.closedtrades

import AppModule
import androidx.compose.runtime.*
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.GetAllClosedTradesDetailed
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import launchUnit
import model.Side
import ui.addclosedtradedetailed.CloseTradeDetailedFormFields
import ui.closedtrades.model.ClosedTradeListItem
import ui.closedtrades.model.ClosedTradesEvent
import ui.closedtrades.model.ClosedTradesEvent.DeleteTrade
import ui.closedtrades.model.ClosedTradesState
import ui.common.CollectEffect
import ui.common.state
import utils.brokerage
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import ui.closedtrades.model.ClosedTradesEvent.DeleteConfirmationDialog as DeleteConfirmationDialogEvent
import ui.closedtrades.model.ClosedTradesEvent.EditTradeWindow as EditTradeWindowEvent
import ui.closedtrades.model.ClosedTradesState.DeleteConfirmationDialog as DeleteConfirmationDialogState
import ui.closedtrades.model.ClosedTradesState.EditTradeWindow as EditTradeWindowState

internal class ClosedTradesPresenter(
    private val coroutineScope: CoroutineScope,
    private val appModule: AppModule,
) {

    private val events = MutableSharedFlow<ClosedTradesEvent>(extraBufferCapacity = Int.MAX_VALUE)

    val state = coroutineScope.launchMolecule(RecompositionClock.ContextClock) {

        return@launchMolecule ClosedTradesState(
            closedTradesItems = getClosedTradeListEntries().value,
            deleteConfirmationDialogState = deleteConfirmationDialogState(events),
            closeTradeDetailedWindowState = closeTradeWindowState(events),
        )
    }

    fun event(event: ClosedTradesEvent) {
        events.tryEmit(event)
    }

    @Composable
    private fun getClosedTradeListEntries(): State<Map<ClosedTradeListItem.DayHeader, List<ClosedTradeListItem.Entry>>> {
        return remember {
            appModule.appDB.closedTradeQueries
                .getAllClosedTradesDetailed()
                .asFlow()
                .mapToList(Dispatchers.IO)
                .map { getAllClosedTradesDetailed ->

                    getAllClosedTradesDetailed
                        .groupBy { LocalDateTime.parse(it.entryDate).date }
                        .mapKeys { (date, _) -> date.toClosedTradeListDayHeader() }
                        .mapValues { (_, list) -> list.map { it.toClosedTradeListEntry() } }
                }
        }.collectAsState(emptyMap())
    }

    private fun LocalDate.toClosedTradeListDayHeader(): ClosedTradeListItem.DayHeader {
        val formatted = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(toJavaLocalDate())
        return ClosedTradeListItem.DayHeader(formatted)
    }

    private fun GetAllClosedTradesDetailed.toClosedTradeListEntry(): ClosedTradeListItem.Entry {

        val instrumentCapitalized = instrument
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val entryBD = entry.toBigDecimal()
        val stopBD = stop?.toBigDecimalOrNull()
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
            null -> null
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
            side = this@toClosedTradeListEntry.side.uppercase(),
            entry = entry,
            stop = stop ?: "NA",
            duration = "${entryDateTime.time} ->\n${exitDateTime.time}\n($duration)",
            target = target ?: "NA",
            exit = exit,
            pnl = pnlBD.toPlainString() + rValue?.let { " (${it}R)" }.orEmpty(),
            isProfitable = pnlBD > BigDecimal.ZERO,
            netPnl = netPnlBD.toPlainString(),
            isNetProfitable = netPnlBD > BigDecimal.ZERO,
            fees = (pnlBD - netPnlBD).toPlainString(),
            maxFavorableExcursion = maxFavorableExcursion.orEmpty(),
            maxAdverseExcursion = maxAdverseExcursion.orEmpty(),
            persisted = persisted.toBoolean(),
            persistenceResult = persistenceResult,
        )
    }

    @Composable
    private fun closeTradeWindowState(events: Flow<ClosedTradesEvent>): EditTradeWindowState {

        val windowEvents = remember(events) { events.filterIsInstance<EditTradeWindowEvent>() }
        var state by state<EditTradeWindowState> { EditTradeWindowState.Closed }

        CollectEffect(windowEvents) { event ->

            if (state is EditTradeWindowState.Open && event is EditTradeWindowEvent.Open)
                return@CollectEffect

            state = when (event) {
                is EditTradeWindowEvent.Open -> {

                    val closedTrade = withContext(Dispatchers.IO) {
                        appModule.appDB.closedTradeQueries.getClosedTradesDetailedById(event.id).executeAsOne()
                    }

                    val model = CloseTradeDetailedFormFields.Model(
                        id = closedTrade.id,
                        ticker = closedTrade.ticker,
                        quantity = closedTrade.quantity,
                        isLong = Side.fromString(closedTrade.side) == Side.Long,
                        entry = closedTrade.entry,
                        stop = closedTrade.stop.orEmpty(),
                        entryDateTime = LocalDateTime.parse(closedTrade.entryDate),
                        target = closedTrade.target.orEmpty(),
                        exit = closedTrade.exit,
                        exitDateTime = LocalDateTime.parse(closedTrade.exitDate),
                        maxFavorableExcursion = closedTrade.maxFavorableExcursion.orEmpty(),
                        maxAdverseExcursion = closedTrade.maxAdverseExcursion.orEmpty(),
                        tags = closedTrade.tags?.split(", ")?.let {
                            if (it.size == 1 && it.first().isBlank()) emptyList() else it
                        } ?: emptyList(),
                        persisted = closedTrade.persisted.toBoolean(),
                    )

                    EditTradeWindowState.Open(model)
                }

                is EditTradeWindowEvent.SaveTrade -> {
                    saveClosedTradeDetailsToDB(event.model)
                    EditTradeWindowState.Closed
                }

                EditTradeWindowEvent.Close -> EditTradeWindowState.Closed
            }
        }

        return state
    }

    private fun saveClosedTradeDetailsToDB(
        model: CloseTradeDetailedFormFields.Model,
    ) = coroutineScope.launchUnit {

        withContext(Dispatchers.IO) {

            val entryTime = model.entryDateTime.time
            val entryDateTime = model.entryDateTime.date.atTime(
                LocalTime(
                    hour = entryTime.hour,
                    minute = entryTime.minute,
                    second = entryTime.second,
                )
            )

            val exitTime = model.exitDateTime.time
            val exitDateTime = model.exitDateTime.date.atTime(
                LocalTime(
                    hour = exitTime.hour,
                    minute = exitTime.minute,
                    second = exitTime.second,
                )
            )

            appModule.appDB.transaction {

                appModule.appDB.closedTradeQueries.insert(
                    id = model.id,
                    broker = "Finvasia",
                    ticker = model.ticker!!,
                    instrument = "equity",
                    quantity = model.quantity,
                    lots = null,
                    side = (if (model.isLong) Side.Long else Side.Short).strValue,
                    entry = model.entry,
                    stop = model.stop,
                    entryDate = entryDateTime.toString(),
                    target = model.target,
                    exit = model.exit,
                    exitDate = exitDateTime.toString(),
                )

                appModule.appDB.closedTradeDetailQueries.insert(
                    closedTradeId = model.id,
                    maxFavorableExcursion = model.maxFavorableExcursion.ifBlank { null },
                    maxAdverseExcursion = model.maxAdverseExcursion.ifBlank { null },
                    tags = model.tags.joinToString(", "),
                    persisted = model.persisted.toString(),
                    persistenceResult = null,
                )
            }
        }
    }

    @Composable
    private fun deleteConfirmationDialogState(events: Flow<ClosedTradesEvent>): DeleteConfirmationDialogState {

        var state by state<DeleteConfirmationDialogState> { DeleteConfirmationDialogState.Dismissed }

        CollectEffect(events) { event ->

            state = when (event) {
                is DeleteTrade -> DeleteConfirmationDialogState.Open(event.id)

                is DeleteConfirmationDialogEvent.Confirm -> {
                    deleteTrade(event.id)
                    DeleteConfirmationDialogState.Dismissed
                }

                DeleteConfirmationDialogEvent.Dismiss -> DeleteConfirmationDialogState.Dismissed
                is EditTradeWindowEvent -> state
            }
        }

        return state
    }

    private fun deleteTrade(id: Int) = coroutineScope.launchUnit {

        withContext(Dispatchers.IO) {
            appModule.appDB.closedTradeQueries.delete(id)
        }
    }
}
