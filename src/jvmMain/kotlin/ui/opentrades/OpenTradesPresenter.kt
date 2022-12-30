package ui.opentrades

import AppModule
import androidx.compose.runtime.*
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.russhwolf.settings.coroutines.FlowSettings
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import fyers_api.FyersApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import launchUnit
import mapList
import model.Side
import ui.closetradeform.CloseTradeFormFields
import ui.closetradeform.CloseTradeFormWindowState
import ui.common.CollectEffect
import ui.opentradeform.OpenTradeFormWindowParams
import ui.opentrades.model.OpenTradeListEntry
import ui.opentrades.model.OpenTradesEvent
import ui.opentrades.model.OpenTradesEvent.DeleteTrade
import ui.opentrades.model.OpenTradesState
import utils.PrefKeys
import java.util.*
import kotlin.time.Duration.Companion.nanoseconds

internal class OpenTradesPresenter(
    private val coroutineScope: CoroutineScope,
    private val appModule: AppModule,
    private val appPrefs: FlowSettings = appModule.appPrefs,
    private val fyersApi: FyersApi = appModule.fyersApiFactory(),
) {

    private val events = MutableSharedFlow<OpenTradesEvent>(extraBufferCapacity = Int.MAX_VALUE)

    private val openTradeFormWindowParams = mutableStateMapOf<UUID, OpenTradeFormWindowParams>()
    private val closeTradeFormWindowStates = mutableStateListOf<CloseTradeFormWindowState>()

    val state = coroutineScope.launchMolecule(RecompositionClock.ContextClock) {

        CollectEffect(events) { event ->

            when (event) {
                OpenTradesEvent.AddTrade -> onAddTrade()
                is OpenTradesEvent.EditTrade -> onEditTrade(event.id)
                is OpenTradesEvent.CloseTrade -> onCloseTrade(event.id)
                is DeleteTrade -> onDeleteTrade(event.id)
            }
        }

        return@launchMolecule OpenTradesState(
            openTrades = getOpenTradeListEntries(),
            openTradeFormWindowParams = openTradeFormWindowParams.values,
            closeTradeFormWindowStates = closeTradeFormWindowStates,
        )
    }

    fun event(event: OpenTradesEvent) {
        events.tryEmit(event)
    }

    @Composable
    private fun getOpenTradeListEntries(): List<OpenTradeListEntry> {
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
        }.collectAsState(emptyList()).value
    }

    private fun onAddTrade() {

        val key = UUID.randomUUID()
        val params = OpenTradeFormWindowParams(
            operationType = OpenTradeFormWindowParams.OperationType.New,
            onCloseRequest = { openTradeFormWindowParams.remove(key) }
        )

        openTradeFormWindowParams[key] = params
    }

    private fun onEditTrade(id: Long) {

        // Don't allow opening duplicate windows
        val isWindowAlreadyOpen = openTradeFormWindowParams.values.any {
            it.operationType is OpenTradeFormWindowParams.OperationType.EditExisting && it.operationType.id == id
        }
        if (isWindowAlreadyOpen) return

        val key = UUID.randomUUID()
        val params = OpenTradeFormWindowParams(
            operationType = OpenTradeFormWindowParams.OperationType.EditExisting(id),
            onCloseRequest = { openTradeFormWindowParams.remove(key) }
        )

        openTradeFormWindowParams[key] = params
    }

    private fun onCloseTrade(id: Long) = coroutineScope.launchUnit {

        // Close trade window already open
        if (closeTradeFormWindowStates.any { it.formModel.id == id }) return@launchUnit

        val openTrade = withContext(Dispatchers.IO) {
            appModule.appDB.openTradeQueries.getById(id).executeAsOne()
        }

        val currentTime = Clock.System.now()
        val currentTimeWithoutNanoseconds = currentTime - currentTime.nanosecondsOfSecond.nanoseconds

        val accessToken = appPrefs.getString(PrefKeys.FyersAccessToken)
        val response = fyersApi.getQuotes(accessToken, listOf("NSE:${openTrade.ticker}-EQ"))
        val currentPrice = response.result?.quote?.first()?.quoteData?.cmd?.close?.toString() ?: "0"

        val model = CloseTradeFormFields.Model(
            id = openTrade.id,
            ticker = openTrade.ticker,
            quantity = openTrade.quantity,
            isLong = Side.fromString(openTrade.side) == Side.Long,
            entry = openTrade.entry,
            stop = openTrade.stop.orEmpty(),
            entryDateTime = LocalDateTime.parse(openTrade.entryDate),
            target = openTrade.target.orEmpty(),
            exit = currentPrice,
            exitDateTime = currentTimeWithoutNanoseconds.toLocalDateTime(TimeZone.currentSystemDefault()),
        )

        closeTradeFormWindowStates += CloseTradeFormWindowState(
            appDB = appModule.appDB,
            formModel = model,
            coroutineScope = coroutineScope,
            onCloseRequest = { closeTradeFormWindowStates.removeIf { it.formModel.id == id } },
        )
    }

    private fun onDeleteTrade(id: Long) = coroutineScope.launchUnit {

        withContext(Dispatchers.IO) {
            appModule.appDB.openTradeQueries.delete(id)
        }
    }
}
