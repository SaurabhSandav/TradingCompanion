package com.saurabhsandav.core.ui.trade

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import com.saurabhsandav.core.LocalAppModule
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.app.AppWindowsManager
import com.saurabhsandav.core.ui.landing.model.LandingState.TradeExecutionFormWindowParams
import com.saurabhsandav.core.ui.trade.model.TradeEvent.*
import com.saurabhsandav.core.ui.trade.model.TradeState
import com.saurabhsandav.core.ui.trade.ui.*

@Composable
internal fun TradeWindow(
    profileId: Long,
    tradeId: Long,
    executionFormWindowsManager: AppWindowsManager<TradeExecutionFormWindowParams>,
    onCloseRequest: () -> Unit,
) {

    val scope = rememberCoroutineScope()
    val appModule = LocalAppModule.current
    val presenter = remember { TradePresenter(profileId, tradeId, scope, appModule, executionFormWindowsManager) }
    val state by presenter.state.collectAsState()

    val windowState = rememberWindowState(
        placement = WindowPlacement.Maximized,
    )

    AppWindow(
        state = windowState,
        title = state.title,
        onCloseRequest = onCloseRequest,
    ) {

        TradeScreen(state)
    }
}

@Composable
internal fun TradeScreen(
    state: TradeState,
) {

    Scaffold {

        when (val details = state.details) {
            null -> CircularProgressIndicator()
            else -> {

                Column(
                    modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {

                    Details(details)

                    if (state.mfeAndMae != null) {
                        MfeAndMae(state.mfeAndMae)
                    }

                    TradeExecutionsTable(
                        items = state.executions,
                        onEditExecution = { executionId -> state.eventSink(EditExecution(executionId)) },
                        onLockExecution = { executionId -> state.eventSink(LockExecution(executionId)) },
                        onDeleteExecution = { executionId -> state.eventSink(DeleteExecution(executionId)) },
                    )

                    StopsAndTargets(
                        stops = state.stops,
                        onAddStop = { state.eventSink(AddStop(it)) },
                        onDeleteStop = { state.eventSink(DeleteStop(it)) },
                        targets = state.targets,
                        onAddTarget = { state.eventSink(AddTarget(it)) },
                        onDeleteTarget = { state.eventSink(DeleteTarget(it)) },
                    )

                    Notes(
                        notes = state.notes,
                        onAddNote = { state.eventSink(AddNote(it)) },
                        onUpdateNote = { id, note -> state.eventSink(UpdateNote(id, note)) },
                        onDeleteNote = { state.eventSink(DeleteNote(it)) },
                    )
                }
            }
        }
    }
}
