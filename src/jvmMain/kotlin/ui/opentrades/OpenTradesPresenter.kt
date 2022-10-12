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
import ui.addclosedtrade.CloseTradeFormFields
import ui.addopentrade.AddOpenTradeFormFields
import ui.common.CollectEffect
import ui.common.state
import ui.opentrades.model.OpenTradeListEntry
import ui.opentrades.model.OpenTradesEvent
import ui.opentrades.model.OpenTradesEvent.DeleteTrade
import ui.opentrades.model.OpenTradesState
import ui.opentrades.model.OpenTradesEvent.AddTradeWindow as AddTradeWindowEvent
import ui.opentrades.model.OpenTradesEvent.CloseTradeWindow as CloseTradeWindowEvent
import ui.opentrades.model.OpenTradesState.AddTradeWindow as AddTradeWindowState
import ui.opentrades.model.OpenTradesState.CloseTradeWindow as CloseTradeWindowState

internal class OpenTradesPresenter(
    private val coroutineScope: CoroutineScope,
    private val appModule: AppModule,
) {

    private val events = MutableSharedFlow<OpenTradesEvent>(extraBufferCapacity = Int.MAX_VALUE)

    val state = coroutineScope.launchMolecule(RecompositionClock.ContextClock) {

        CollectEffect(events) { event ->

            when (event) {
                is DeleteTrade -> deleteTrade(event.id)
                is AddTradeWindowEvent, is CloseTradeWindowEvent -> Unit
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

        val addTradeWindowEvents = remember(events) { events.filterIsInstance<AddTradeWindowEvent>() }
        var addTradeWindowState by state<AddTradeWindowState> { AddTradeWindowState.Closed }

        CollectEffect(addTradeWindowEvents) { event ->

            if (addTradeWindowState is AddTradeWindowState.Open
                && (event is AddTradeWindowEvent.Open || event is AddTradeWindowEvent.OpenEdit)
            ) return@CollectEffect

            addTradeWindowState = when (event) {
                AddTradeWindowEvent.Open -> AddTradeWindowState.Open(AddOpenTradeFormFields.Model())
                is AddTradeWindowEvent.OpenEdit -> {

                    val openTrade = withContext(Dispatchers.IO) {
                        appModule.appDB.openTradeQueries.getById(event.id).executeAsOne()
                    }

                    val model = AddOpenTradeFormFields.Model(
                        id = openTrade.id,
                        ticker = openTrade.ticker,
                        quantity = openTrade.quantity,
                        isLong = Side.fromString(openTrade.side) == Side.Long,
                        entry = openTrade.entry,
                        stop = openTrade.stop.orEmpty(),
                        entryDateTime = LocalDateTime.parse(openTrade.entryDate),
                        target = openTrade.target.orEmpty(),
                    )

                    AddTradeWindowState.Open(model)
                }

                is AddTradeWindowEvent.SaveTrade -> {
                    saveOpenTradeToDB(event.model)
                    AddTradeWindowState.Closed
                }

                AddTradeWindowEvent.Close -> AddTradeWindowState.Closed
            }
        }

        return addTradeWindowState
    }

    @Composable
    private fun closeTradeWindowState(events: Flow<OpenTradesEvent>): CloseTradeWindowState {

        val windowEvents = remember(events) { events.filterIsInstance<CloseTradeWindowEvent>() }
        var state by state<CloseTradeWindowState> { CloseTradeWindowState.Closed }

        CollectEffect(windowEvents) { event ->

            if (state is CloseTradeWindowState.Open && event is CloseTradeWindowEvent.Open)
                return@CollectEffect

            state = when (event) {
                is CloseTradeWindowEvent.Open -> {

                    val openTrade = withContext(Dispatchers.IO) {
                        appModule.appDB.openTradeQueries.getById(event.id).executeAsOne()
                    }

                    val model = CloseTradeFormFields.Model(
                        id = openTrade.id,
                        ticker = openTrade.ticker,
                        quantity = openTrade.quantity,
                        isLong = Side.fromString(openTrade.side) == Side.Long,
                        entry = openTrade.entry,
                        stop = openTrade.stop.orEmpty(),
                        entryDateTime = LocalDateTime.parse(openTrade.entryDate),
                        target = openTrade.target.orEmpty(),
                    )

                    CloseTradeWindowState.Open(model)
                }

                is CloseTradeWindowEvent.SaveTrade -> {
                    saveClosedTradeToDB(event.model)
                    CloseTradeWindowState.Closed
                }

                CloseTradeWindowEvent.Close -> CloseTradeWindowState.Closed
            }
        }

        return state
    }

    private fun saveOpenTradeToDB(
        model: AddOpenTradeFormFields.Model,
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
                ticker = model.ticker!!,
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
        model: CloseTradeFormFields.Model,
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
                    id = null,
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

                appModule.appDB.openTradeQueries.delete(model.id!!)
            }
        }
    }

    private fun deleteTrade(id: Int) = coroutineScope.launchUnit {

        withContext(Dispatchers.IO) {
            appModule.appDB.openTradeQueries.delete(id)
        }
    }
}
