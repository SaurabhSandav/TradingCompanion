package com.saurabhsandav.core.ui.opentrades

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.launchUnit
import com.saurabhsandav.core.mapList
import com.saurabhsandav.core.ui.closetradeform.CloseTradeFormWindowParams
import com.saurabhsandav.core.ui.common.CollectEffect
import com.saurabhsandav.core.ui.opentradeform.OpenTradeFormWindowParams
import com.saurabhsandav.core.ui.opentrades.model.OpenTradeListEntry
import com.saurabhsandav.core.ui.opentrades.model.OpenTradesEvent
import com.saurabhsandav.core.ui.opentrades.model.OpenTradesEvent.DeleteTrade
import com.saurabhsandav.core.ui.opentrades.model.OpenTradesState
import com.saurabhsandav.core.ui.pnlcalculator.PNLCalculatorWindowParams
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import java.util.*

internal class OpenTradesPresenter(
    private val coroutineScope: CoroutineScope,
    private val appModule: AppModule,
) {

    private val events = MutableSharedFlow<OpenTradesEvent>(extraBufferCapacity = Int.MAX_VALUE)

    private val openTradeFormWindowParams = mutableStateMapOf<UUID, OpenTradeFormWindowParams>()
    private val pnlCalculatorWindowParams = mutableStateMapOf<UUID, PNLCalculatorWindowParams>()
    private val closeTradeFormWindowParams = mutableStateMapOf<UUID, CloseTradeFormWindowParams>()

    val state = coroutineScope.launchMolecule(RecompositionClock.ContextClock) {

        CollectEffect(events) { event ->

            when (event) {
                OpenTradesEvent.AddTrade -> onAddTrade()
                is OpenTradesEvent.EditTrade -> onEditTrade(event.id)
                is OpenTradesEvent.OpenPNLCalculator -> onOpenPNLCalculator(event.id)
                is OpenTradesEvent.CloseTrade -> onCloseTrade(event.id)
                is DeleteTrade -> onDeleteTrade(event.id)
            }
        }

        return@launchMolecule OpenTradesState(
            openTrades = getOpenTradeListEntries(),
            openTradeFormWindowParams = openTradeFormWindowParams.values,
            pnlCalculatorWindowParams = pnlCalculatorWindowParams.values,
            closeTradeFormWindowParams = closeTradeFormWindowParams.values,
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

                    val instrumentCapitalized = openTrade.instrument
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

                    OpenTradeListEntry(
                        id = openTrade.id,
                        broker = "${openTrade.broker} ($instrumentCapitalized)",
                        ticker = openTrade.ticker,
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

    private fun onOpenPNLCalculator(id: Long) {

        // Don't allow opening duplicate windows
        val isWindowAlreadyOpen = pnlCalculatorWindowParams.values.any {
            it.operationType is PNLCalculatorWindowParams.OperationType.FromOpenTrade && it.operationType.id == id
        }
        if (isWindowAlreadyOpen) return

        val key = UUID.randomUUID()
        val params = PNLCalculatorWindowParams(
            operationType = PNLCalculatorWindowParams.OperationType.FromOpenTrade(id),
            onCloseRequest = { pnlCalculatorWindowParams.remove(key) }
        )

        pnlCalculatorWindowParams[key] = params
    }

    private fun onCloseTrade(id: Long) {

        // Don't allow opening duplicate windows
        val isWindowAlreadyOpen = closeTradeFormWindowParams.values.any {
            it.operationType is CloseTradeFormWindowParams.OperationType.CloseOpenTrade && it.operationType.openTradeId == id
        }
        if (isWindowAlreadyOpen) return

        val key = UUID.randomUUID()
        val params = CloseTradeFormWindowParams(
            operationType = CloseTradeFormWindowParams.OperationType.CloseOpenTrade(id),
            onCloseRequest = { closeTradeFormWindowParams.remove(key) }
        )

        closeTradeFormWindowParams[key] = params
    }

    private fun onDeleteTrade(id: Long) = coroutineScope.launchUnit {

        withContext(Dispatchers.IO) {
            appModule.appDB.openTradeQueries.delete(id)
        }
    }
}
