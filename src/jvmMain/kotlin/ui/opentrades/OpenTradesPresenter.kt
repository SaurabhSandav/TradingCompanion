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
import ui.common.CollectEffect
import ui.common.state

internal class OpenTradesPresenter(
    private val coroutineScope: CoroutineScope,
    private val appModule: AppModule,
) {

    private val events = MutableSharedFlow<OpenTradesEvent>(extraBufferCapacity = Int.MAX_VALUE)

    val state = coroutineScope.launchMolecule(RecompositionClock.ContextClock) {

        CollectEffect(events) { event ->
            when (event) {
                is OpenTradesEvent.AddNewTrade -> {
                    addTrade(event.model)
                    event(OpenTradesEvent.AddTradeWindow.Close)
                }

                is OpenTradesEvent.AddTradeWindow -> Unit
            }
        }

        val addTradeWindowEvents = remember(events) { events.filterIsInstance<OpenTradesEvent.AddTradeWindow>() }

        return@launchMolecule OpenTradesState(
            openTrades = getOpenTradeListEntries().value,
            addTradeWindowState = addTradeWindowState(addTradeWindowEvents),
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
    private fun addTradeWindowState(events: Flow<OpenTradesEvent.AddTradeWindow>): AddTradeWindowState {

        var addTradeWindowState by state<AddTradeWindowState> { AddTradeWindowState.Closed }

        CollectEffect(events) { event ->
            addTradeWindowState = when (event) {
                OpenTradesEvent.AddTradeWindow.AddTrade -> AddTradeWindowState.Open()
                is OpenTradesEvent.AddTradeWindow.EditTrade -> {

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

                OpenTradesEvent.AddTradeWindow.Close -> AddTradeWindowState.Closed
            }
        }

        return addTradeWindowState
    }

    private fun addTrade(
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
}
