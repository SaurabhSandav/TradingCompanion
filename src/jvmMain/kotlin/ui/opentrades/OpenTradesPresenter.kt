package ui.opentrades

import AppModule
import androidx.compose.runtime.*
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atTime
import launchUnit
import mapList
import model.Side
import ui.addclosedtrade.CloseTradeFormState
import ui.addopentrade.AddOpenTradeFormState
import ui.common.CollectEffect
import ui.common.state
import ui.opentrades.model.*
import ui.opentrades.model.OpenTradesEvent.AddTradeWindow
import ui.opentrades.model.OpenTradesEvent.CloseTradeWindow

internal class OpenTradesPresenter(
    private val coroutineScope: CoroutineScope,
    private val appModule: AppModule,
) {

    private val events = MutableSharedFlow<OpenTradesEvent>(extraBufferCapacity = Int.MAX_VALUE)

    val state = coroutineScope.launchMolecule(RecompositionClock.ContextClock) {

        CollectEffect(events) { event ->

            when (event) {
                is OpenTradesEvent.DeleteTrade -> deleteTrade(event.id)
                is AddTradeWindow, is CloseTradeWindow -> Unit
            }
        }

        return@launchMolecule OpenTradesState(
            openTrades = getOpenTradeListEntries().value,
            addTradeWindowState = addTradeWindowState(events),
            closeTradeWindowState = closeTradeWindowState(events),
        )
    }

    fun event(event: OpenTradesEvent) {
        events.tryEmit(event)
    }

    @Composable
    private fun getOpenTradeListEntries(): State<List<OpenTradeListEntry>> {
        return remember {
            appModule.appDB.openTradeQueries
                .getAll()
                .asFlow()
                .mapToList(Dispatchers.IO)
                .mapList { openTrade ->

                    val entryDateTime = LocalDateTime.parse(openTrade.entryDate)

                    OpenTradeListEntry(
                        id = openTrade.id,
                        broker = openTrade.broker,
                        ticker = openTrade.ticker,
                        instrument = openTrade.instrument,
                        quantity = openTrade.lots?.let { "${openTrade.quantity} ($it ${if (it == 1) "lot" else "lots"})" }
                            ?: openTrade.quantity,
                        side = openTrade.side.uppercase(),
                        entry = openTrade.entry,
                        stop = openTrade.stop ?: "NA",
                        entryTime = entryDateTime.time.toString(),
                        target = openTrade.target ?: "NA",
                    )
                }
        }.collectAsState(emptyList())
    }

    @Composable
    private fun addTradeWindowState(events: Flow<OpenTradesEvent>): AddTradeWindowState {

        val addTradeWindowEvents = remember(events) { events.filterIsInstance<AddTradeWindow>() }
        var addTradeWindowState by state<AddTradeWindowState> { AddTradeWindowState.Closed }

        CollectEffect(addTradeWindowEvents) { event ->

            if (addTradeWindowState is AddTradeWindowState.Open
                && (event is AddTradeWindow.Open || event is AddTradeWindow.OpenEdit)
            ) return@CollectEffect

            addTradeWindowState = when (event) {
                AddTradeWindow.Open -> AddTradeWindowState.Open()
                is AddTradeWindow.OpenEdit -> {

                    val openTrade = withContext(Dispatchers.IO) {
                        appModule.appDB.openTradeQueries.getById(event.id).executeAsOne()
                    }

                    val model = AddOpenTradeFormState.Model(
                        id = openTrade.id,
                        ticker = openTrade.ticker,
                        quantity = openTrade.quantity,
                        isLong = Side.fromString(openTrade.side) == Side.Long,
                        entry = openTrade.entry,
                        stop = openTrade.stop ?: "",
                        entryDateTime = LocalDateTime.parse(openTrade.entryDate),
                        target = openTrade.target ?: "",
                    )

                    AddTradeWindowState.Open(model)
                }

                is AddTradeWindow.SaveTrade -> {
                    saveOpenTradeToDB(event.model)
                    AddTradeWindowState.Closed
                }

                AddTradeWindow.Close -> AddTradeWindowState.Closed
            }
        }

        return addTradeWindowState
    }

    @Composable
    private fun closeTradeWindowState(events: Flow<OpenTradesEvent>): CloseTradeWindowState {

        val windowEvents = remember(events) { events.filterIsInstance<CloseTradeWindow>() }
        var state by state<CloseTradeWindowState> { CloseTradeWindowState.Closed }

        CollectEffect(windowEvents) { event ->

            if (state is CloseTradeWindowState.Open && event is CloseTradeWindow.Open)
                return@CollectEffect

            state = when (event) {
                is CloseTradeWindow.Open -> {

                    val openTrade = withContext(Dispatchers.IO) {
                        appModule.appDB.openTradeQueries.getById(event.id).executeAsOne()
                    }

                    val model = AddOpenTradeFormState.Model(
                        id = openTrade.id,
                        ticker = openTrade.ticker,
                        quantity = openTrade.quantity,
                        isLong = Side.fromString(openTrade.side) == Side.Long,
                        entry = openTrade.entry,
                        stop = openTrade.stop ?: "",
                        entryDateTime = LocalDateTime.parse(openTrade.entryDate),
                        target = openTrade.target ?: "",
                    )

                    CloseTradeWindowState.Open(model)
                }

                is CloseTradeWindow.SaveTrade -> {
                    saveClosedTradeToDB(event.model)
                    CloseTradeWindowState.Closed
                }

                CloseTradeWindow.Close -> CloseTradeWindowState.Closed
            }
        }

        return state
    }

    private fun saveOpenTradeToDB(
        model: AddOpenTradeFormState.Model,
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

            appModule.appDB.openTradeQueries.insert(
                id = model.id,
                broker = "Finvasia",
                ticker = model.ticker,
                instrument = "equity",
                quantity = model.quantity,
                lots = null,
                side = (if (model.isLong) Side.Long else Side.Short).strValue,
                entry = model.entry,
                stop = model.stop,
                entryDate = entryDateTime.toString(),
                target = model.target,
            )
        }
    }

    private fun saveClosedTradeToDB(
        model: CloseTradeFormState.Model,
    ) = coroutineScope.launchUnit {

        withContext(Dispatchers.IO) {

            val entryTime = model.openTradeModel.entryDateTime.time
            val entryDateTime = model.openTradeModel.entryDateTime.date.atTime(
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
                    broker = "Finvasia",
                    ticker = model.openTradeModel.ticker,
                    instrument = "equity",
                    quantity = model.openTradeModel.quantity,
                    lots = null,
                    side = (if (model.openTradeModel.isLong) Side.Long else Side.Short).strValue,
                    entry = model.openTradeModel.entry,
                    stop = model.openTradeModel.stop,
                    entryDate = entryDateTime.toString(),
                    target = model.openTradeModel.target,
                    exit = model.exit,
                    exitDate = exitDateTime.toString(),
                )

                appModule.appDB.openTradeQueries.delete(model.openTradeModel.id!!)
            }
        }
    }

    private fun deleteTrade(id: Int) = coroutineScope.launchUnit {

        withContext(Dispatchers.IO) {
            appModule.appDB.openTradeQueries.delete(id)
        }
    }
}
