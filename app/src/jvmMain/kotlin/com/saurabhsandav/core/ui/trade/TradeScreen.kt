package com.saurabhsandav.core.ui.trade

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.WindowPlacement
import com.saurabhsandav.core.LocalScreensModule
import com.saurabhsandav.core.trades.model.AttachmentFileId
import com.saurabhsandav.core.trades.model.TradeExecutionId
import com.saurabhsandav.core.trades.model.TradeNoteId
import com.saurabhsandav.core.trades.model.TradeTagId
import com.saurabhsandav.core.ui.common.BoxWithScrollbar
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.app.rememberAppWindowState
import com.saurabhsandav.core.ui.tags.model.TradeTag
import com.saurabhsandav.core.ui.theme.dimens
import com.saurabhsandav.core.ui.trade.model.TradeEvent.AddNote
import com.saurabhsandav.core.ui.trade.model.TradeEvent.AddStop
import com.saurabhsandav.core.ui.trade.model.TradeEvent.AddTag
import com.saurabhsandav.core.ui.trade.model.TradeEvent.AddTarget
import com.saurabhsandav.core.ui.trade.model.TradeEvent.AddToTrade
import com.saurabhsandav.core.ui.trade.model.TradeEvent.CloseTrade
import com.saurabhsandav.core.ui.trade.model.TradeEvent.DeleteExecution
import com.saurabhsandav.core.ui.trade.model.TradeEvent.DeleteNote
import com.saurabhsandav.core.ui.trade.model.TradeEvent.DeleteStop
import com.saurabhsandav.core.ui.trade.model.TradeEvent.DeleteTarget
import com.saurabhsandav.core.ui.trade.model.TradeEvent.EditExecution
import com.saurabhsandav.core.ui.trade.model.TradeEvent.LockExecution
import com.saurabhsandav.core.ui.trade.model.TradeEvent.NewFromExistingExecution
import com.saurabhsandav.core.ui.trade.model.TradeEvent.OpenChart
import com.saurabhsandav.core.ui.trade.model.TradeEvent.RemoveAttachment
import com.saurabhsandav.core.ui.trade.model.TradeEvent.RemoveTag
import com.saurabhsandav.core.ui.trade.model.TradeEvent.SetPrimaryStop
import com.saurabhsandav.core.ui.trade.model.TradeEvent.SetPrimaryTarget
import com.saurabhsandav.core.ui.trade.model.TradeEvent.UpdateNote
import com.saurabhsandav.core.ui.trade.model.TradeState.Details
import com.saurabhsandav.core.ui.trade.model.TradeState.Excursions
import com.saurabhsandav.core.ui.trade.model.TradeState.Execution
import com.saurabhsandav.core.ui.trade.model.TradeState.TradeAttachment
import com.saurabhsandav.core.ui.trade.model.TradeState.TradeNote
import com.saurabhsandav.core.ui.trade.model.TradeState.TradeStop
import com.saurabhsandav.core.ui.trade.model.TradeState.TradeTarget
import com.saurabhsandav.core.ui.trade.ui.Attachments
import com.saurabhsandav.core.ui.trade.ui.Details
import com.saurabhsandav.core.ui.trade.ui.Excursions
import com.saurabhsandav.core.ui.trade.ui.ExecutionsTable
import com.saurabhsandav.core.ui.trade.ui.Notes
import com.saurabhsandav.core.ui.trade.ui.StopsAndTargets
import com.saurabhsandav.core.ui.trade.ui.Tags
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

@Composable
internal fun TradeWindow(
    profileTradeId: ProfileTradeId,
    onCloseRequest: () -> Unit,
) {

    val scope = rememberCoroutineScope()
    val screensModule = LocalScreensModule.current
    val presenter = remember { screensModule.tradeModule(scope).presenter(profileTradeId, onCloseRequest) }
    val state by presenter.state.collectAsState()

    val windowState = rememberAppWindowState(
        preferredPlacement = WindowPlacement.Maximized,
    )

    val details = state.details ?: return

    AppWindow(
        state = windowState,
        title = state.title,
        onCloseRequest = onCloseRequest,
    ) {

        TradeScreen(
            profileTradeId = profileTradeId,
            details = details,
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
            stopPreviewer = state.stopPreviewer,
            onAddStop = { state.eventSink(AddStop(it)) },
            onDeleteStop = { state.eventSink(DeleteStop(it)) },
            onSetPrimaryStop = { state.eventSink(SetPrimaryStop(it)) },
            targets = state.targets,
            showTargetRValues = state.showTargetRValues,
            targetPreviewer = state.targetPreviewer,
            onAddTarget = { state.eventSink(AddTarget(it)) },
            onDeleteTarget = { state.eventSink(DeleteTarget(it)) },
            onSetPrimaryTarget = { state.eventSink(SetPrimaryTarget(it)) },
            excursions = state.excursions,
            tags = state.tags,
            onAddTag = { id -> state.eventSink(AddTag(id)) },
            onRemoveTag = { id -> state.eventSink(RemoveTag(id)) },
            attachments = state.attachments,
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
    profileTradeId: ProfileTradeId,
    details: Details,
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
    stopPreviewer: Flow<StopPreviewer>,
    onAddStop: (BigDecimal) -> Unit,
    onDeleteStop: (BigDecimal) -> Unit,
    onSetPrimaryStop: (BigDecimal) -> Unit,
    targets: List<TradeTarget>,
    showTargetRValues: Boolean,
    targetPreviewer: Flow<TargetPreviewer>,
    onAddTarget: (BigDecimal) -> Unit,
    onDeleteTarget: (BigDecimal) -> Unit,
    onSetPrimaryTarget: (BigDecimal) -> Unit,
    excursions: Excursions?,
    tags: List<TradeTag>,
    onAddTag: (TradeTagId) -> Unit,
    onRemoveTag: (TradeTagId) -> Unit,
    attachments: List<TradeAttachment>,
    onRemoveAttachment: (AttachmentFileId) -> Unit,
    notes: List<TradeNote>,
    onAddNote: (note: String, isMarkdown: Boolean) -> Unit,
    onUpdateNote: (id: TradeNoteId, note: String, isMarkdown: Boolean) -> Unit,
    onDeleteNote: (id: TradeNoteId) -> Unit,
) {

    Scaffold {

        val scrollState = rememberScrollState()

        BoxWithScrollbar(
            modifier = Modifier.fillMaxSize(),
            scrollbarAdapter = rememberScrollbarAdapter(scrollState),
        ) {

            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(MaterialTheme.dimens.containerPadding),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.columnVerticalSpacing),
            ) {

                Details(
                    modifier = Modifier.fillMaxWidth(),
                    details = details,
                    onOpenChart = onOpenChart,
                )

                ExecutionsTable(
                    items = executions,
                    newExecutionEnabled = newExecutionEnabled,
                    onAddToTrade = onAddToTrade,
                    onCloseTrade = onCloseTrade,
                    onNewFromExistingExecution = onNewFromExistingExecution,
                    onEditExecution = onEditExecution,
                    onLockExecution = onLockExecution,
                    onDeleteExecution = onDeleteExecution,
                )

                StopsAndTargets(
                    stops = stops,
                    stopPreviewer = stopPreviewer,
                    onAddStop = onAddStop,
                    onDeleteStop = onDeleteStop,
                    onSetPrimaryStop = onSetPrimaryStop,
                    targets = targets,
                    showTargetRValues = showTargetRValues,
                    targetPreviewer = targetPreviewer,
                    onAddTarget = onAddTarget,
                    onDeleteTarget = onDeleteTarget,
                    onSetPrimaryTarget = onSetPrimaryTarget,
                )

                if (excursions != null) {

                    Excursions(
                        modifier = Modifier.fillMaxWidth(),
                        excursions = excursions,
                    )
                }

                Tags(
                    profileTradeId = profileTradeId,
                    tags = tags,
                    onAddTag = onAddTag,
                    onRemoveTag = onRemoveTag,
                )

                Attachments(
                    profileTradeId = profileTradeId,
                    attachments = attachments,
                    onRemoveAttachment = onRemoveAttachment,
                )

                Notes(
                    notes = notes,
                    onAddNote = onAddNote,
                    onUpdateNote = onUpdateNote,
                    onDeleteNote = onDeleteNote,
                )
            }
        }
    }
}
