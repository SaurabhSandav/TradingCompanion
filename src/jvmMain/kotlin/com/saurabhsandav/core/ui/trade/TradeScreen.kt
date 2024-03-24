package com.saurabhsandav.core.ui.trade

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.WindowPlacement
import com.saurabhsandav.core.LocalAppModule
import com.saurabhsandav.core.trades.model.TradeAttachmentId
import com.saurabhsandav.core.trades.model.TradeExecutionId
import com.saurabhsandav.core.trades.model.TradeNoteId
import com.saurabhsandav.core.trades.model.TradeTagId
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.app.rememberAppWindowState
import com.saurabhsandav.core.ui.theme.dimens
import com.saurabhsandav.core.ui.trade.model.AttachmentFormModel
import com.saurabhsandav.core.ui.trade.model.TradeEvent.*
import com.saurabhsandav.core.ui.trade.model.TradeState.*
import com.saurabhsandav.core.ui.trade.ui.*
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

@Composable
internal fun TradeWindow(
    profileTradeId: ProfileTradeId,
    onCloseRequest: () -> Unit,
) {

    val scope = rememberCoroutineScope()
    val appModule = LocalAppModule.current
    val presenter = remember { appModule.tradeModule(scope).presenter(profileTradeId, onCloseRequest) }
    val state by presenter.state.collectAsState()

    val windowState = rememberAppWindowState(
        preferredPlacement = WindowPlacement.Maximized,
    )

    AppWindow(
        state = windowState,
        title = state.title,
        onCloseRequest = onCloseRequest,
    ) {

        TradeScreen(
            details = state.details,
            executions = state.executions,
            newExecutionEnabled = state.newExecutionEnabled,
            onAddToTrade = { state.eventSink(AddToTrade) },
            onCloseTrade = { state.eventSink(CloseTrade) },
            onNewFromExistingExecution = { fromExecutionId ->
                state.eventSink(NewFromExistingExecution(fromExecutionId))
            },
            onEditExecution = { executionId -> state.eventSink(EditExecution(executionId)) },
            onLockExecution = { executionId -> state.eventSink(LockExecution(executionId)) },
            onDeleteExecution = { executionId -> state.eventSink(DeleteExecution(executionId)) },
            onOpenChart = { state.eventSink(OpenChart) },
            stops = state.stops,
            previewStop = state.previewStop,
            onAddStop = { state.eventSink(AddStop(it)) },
            onDeleteStop = { state.eventSink(DeleteStop(it)) },
            targets = state.targets,
            previewTarget = state.previewTarget,
            onAddTarget = { state.eventSink(AddTarget(it)) },
            onDeleteTarget = { state.eventSink(DeleteTarget(it)) },
            excursions = state.excursions,
            tags = state.tags,
            tagSuggestions = state.tagSuggestions,
            onAddTag = { id -> state.eventSink(AddTag(id)) },
            onRemoveTag = { id -> state.eventSink(RemoveTag(id)) },
            attachments = state.attachments,
            onAddAttachment = { formModel -> state.eventSink(AddAttachment(formModel)) },
            onUpdateAttachment = { id, formModel -> state.eventSink(UpdateAttachment(id, formModel)) },
            onRemoveAttachment = { id -> state.eventSink(RemoveAttachment(id)) },
            notes = state.notes,
            onAddNote = { note, isMarkdown -> state.eventSink(AddNote(note, isMarkdown)) },
            onUpdateNote = { id, note, isMarkdown -> state.eventSink(UpdateNote(id, note, isMarkdown)) },
            onDeleteNote = { state.eventSink(DeleteNote(it)) },
        )
    }
}

@Composable
internal fun TradeScreen(
    details: Details?,
    executions: List<Execution>,
    newExecutionEnabled: Boolean,
    onAddToTrade: () -> Unit,
    onCloseTrade: () -> Unit,
    onNewFromExistingExecution: (TradeExecutionId) -> Unit,
    onEditExecution: (TradeExecutionId) -> Unit,
    onLockExecution: (TradeExecutionId) -> Unit,
    onDeleteExecution: (TradeExecutionId) -> Unit,
    onOpenChart: () -> Unit,
    stops: List<TradeStop>,
    previewStop: (BigDecimal) -> Flow<TradeStop?>,
    onAddStop: (BigDecimal) -> Unit,
    onDeleteStop: (BigDecimal) -> Unit,
    targets: List<TradeTarget>,
    previewTarget: (BigDecimal) -> Flow<TradeTarget?>,
    onAddTarget: (BigDecimal) -> Unit,
    onDeleteTarget: (BigDecimal) -> Unit,
    excursions: Excursions?,
    tags: List<TradeTag>,
    tagSuggestions: (String) -> Flow<List<TradeTag>>,
    onAddTag: (TradeTagId) -> Unit,
    onRemoveTag: (TradeTagId) -> Unit,
    attachments: List<TradeAttachment>,
    onAddAttachment: (AttachmentFormModel) -> Unit,
    onUpdateAttachment: (TradeAttachmentId, AttachmentFormModel) -> Unit,
    onRemoveAttachment: (TradeAttachmentId) -> Unit,
    notes: List<TradeNote>,
    onAddNote: (note: String, isMarkdown: Boolean) -> Unit,
    onUpdateNote: (id: TradeNoteId, note: String, isMarkdown: Boolean) -> Unit,
    onDeleteNote: (id: TradeNoteId) -> Unit,
) {

    Scaffold {

        Box(Modifier.fillMaxSize()) {

            when (details) {
                null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                else -> {

                    val scrollState = rememberScrollState()

                    Column(
                        modifier = Modifier.padding(MaterialTheme.dimens.containerPadding).verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.columnVerticalSpacing),
                    ) {

                        Details(details)

                        TradeExecutionsTable(
                            items = executions,
                            newExecutionEnabled = newExecutionEnabled,
                            onAddToTrade = onAddToTrade,
                            onCloseTrade = onCloseTrade,
                            onNewFromExistingExecution = onNewFromExistingExecution,
                            onEditExecution = onEditExecution,
                            onLockExecution = onLockExecution,
                            onDeleteExecution = onDeleteExecution,
                        )

                        FilledTonalButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onOpenChart,
                            content = { Text("Chart") },
                        )

                        StopsAndTargets(
                            stops = stops,
                            previewStop = previewStop,
                            onAddStop = onAddStop,
                            onDeleteStop = onDeleteStop,
                            targets = targets,
                            previewTarget = previewTarget,
                            onAddTarget = onAddTarget,
                            onDeleteTarget = onDeleteTarget,
                        )

                        if (excursions != null) {
                            Excursions(excursions)
                        }

                        Tags(
                            tags = tags,
                            tagSuggestions = tagSuggestions,
                            onAddTag = onAddTag,
                            onRemoveTag = onRemoveTag,
                        )

                        Attachments(
                            attachments = attachments,
                            onAddAttachment = onAddAttachment,
                            onUpdateAttachment = onUpdateAttachment,
                            onRemoveAttachment = onRemoveAttachment,
                        )

                        Notes(
                            notes = notes,
                            onAddNote = onAddNote,
                            onUpdateNote = onUpdateNote,
                            onDeleteNote = onDeleteNote,
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
