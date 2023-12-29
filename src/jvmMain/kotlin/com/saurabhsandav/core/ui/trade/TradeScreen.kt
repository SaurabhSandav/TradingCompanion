package com.saurabhsandav.core.ui.trade

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import com.saurabhsandav.core.LocalAppModule
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.app.rememberAppWindowState
import com.saurabhsandav.core.ui.trade.model.TradeEvent.*
import com.saurabhsandav.core.ui.trade.model.TradeState
import com.saurabhsandav.core.ui.trade.ui.*
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId

@Composable
internal fun TradeWindow(
    profileTradeId: ProfileTradeId,
    onCloseRequest: () -> Unit,
) {

    val scope = rememberCoroutineScope()
    val appModule = LocalAppModule.current
    val presenter = remember { appModule.tradeModule(scope).presenter(profileTradeId) }
    val state by presenter.state.collectAsState()

    val windowState = rememberAppWindowState(
        defaultPlacement = WindowPlacement.Maximized,
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

        Box(Modifier.fillMaxSize()) {

            when (val details = state.details) {
                null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                else -> {

                    val scrollState = rememberScrollState()

                    Column(
                        modifier = Modifier.padding(16.dp).verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {

                        Details(details)

                        TradeExecutionsTable(
                            items = state.executions,
                            newExecutionEnabled = state.newExecutionEnabled,
                            onAddToTrade = { state.eventSink(AddToTrade) },
                            onCloseTrade = { state.eventSink(CloseTrade) },
                            onNewFromExistingExecution = { fromExecutionId ->
                                state.eventSink(NewFromExistingExecution(fromExecutionId))
                            },
                            onEditExecution = { executionId -> state.eventSink(EditExecution(executionId)) },
                            onLockExecution = { executionId -> state.eventSink(LockExecution(executionId)) },
                            onDeleteExecution = { executionId -> state.eventSink(DeleteExecution(executionId)) },
                        )

                        FilledTonalButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { state.eventSink(OpenChart) },
                            content = { Text("Chart") },
                        )

                        StopsAndTargets(
                            stops = state.stops,
                            previewStop = state.previewStop,
                            onAddStop = { state.eventSink(AddStop(it)) },
                            onDeleteStop = { state.eventSink(DeleteStop(it)) },
                            targets = state.targets,
                            previewTarget = state.previewTarget,
                            onAddTarget = { state.eventSink(AddTarget(it)) },
                            onDeleteTarget = { state.eventSink(DeleteTarget(it)) },
                        )

                        if (state.excursions != null) {
                            Excursions(state.excursions)
                        }

                        Tags(
                            tags = state.tags,
                            tagSuggestions = state.tagSuggestions,
                            onAddTag = { id -> state.eventSink(AddTag(id)) },
                            onRemoveTag = { id -> state.eventSink(RemoveTag(id)) },
                        )

                        Attachments(
                            attachments = state.attachments,
                            onAddAttachment = { formModel -> state.eventSink(AddAttachment(formModel)) },
                            onUpdateAttachment = { id, formModel -> state.eventSink(UpdateAttachment(id, formModel)) },
                            onRemoveAttachment = { id -> state.eventSink(RemoveAttachment(id)) },
                        )

                        Notes(
                            notes = state.notes,
                            onAddNote = { note, isMarkdown -> state.eventSink(AddNote(note, isMarkdown)) },
                            onUpdateNote = { id, note, isMarkdown ->
                                state.eventSink(UpdateNote(id, note, isMarkdown))
                            },
                            onDeleteNote = { state.eventSink(DeleteNote(it)) },
                        )
                    }

                    VerticalScrollbar(
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(scrollState)
                    )
                }
            }
        }
    }
}
