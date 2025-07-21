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
import com.saurabhsandav.core.LocalAppGraph
import com.saurabhsandav.core.ui.common.BoxWithScrollbar
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.app.rememberAppWindowState
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
import com.saurabhsandav.core.ui.trade.ui.Attachments
import com.saurabhsandav.core.ui.trade.ui.Details
import com.saurabhsandav.core.ui.trade.ui.Excursions
import com.saurabhsandav.core.ui.trade.ui.ExecutionsTable
import com.saurabhsandav.core.ui.trade.ui.Notes
import com.saurabhsandav.core.ui.trade.ui.StopsAndTargets
import com.saurabhsandav.core.ui.trade.ui.Tags
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId

@Composable
internal fun TradeWindow(
    profileTradeId: ProfileTradeId,
    onCloseRequest: () -> Unit,
) {

    val scope = rememberCoroutineScope()
    val appGraph = LocalAppGraph.current
    val presenter = remember {
        appGraph.tradeGraphFactory
            .create(profileTradeId)
            .presenterFactory
            .create(onCloseRequest, scope)
    }
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
            details = {

                Details(
                    modifier = Modifier.fillMaxWidth(),
                    details = details,
                    onOpenChart = { state.eventSink(OpenChart) },
                )
            },
            executions = {

                ExecutionsTable(
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
            },
            stopsAndTargets = {

                StopsAndTargets(
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
                )
            },
            excursions = {

                val excursions = state.excursions
                if (excursions != null) {

                    Excursions(
                        modifier = Modifier.fillMaxWidth(),
                        excursions = excursions,
                    )
                }
            },
            tags = {

                Tags(
                    profileTradeId = profileTradeId,
                    tags = state.tags,
                    onAddTag = { id -> state.eventSink(AddTag(id)) },
                    onRemoveTag = { id -> state.eventSink(RemoveTag(id)) },
                )
            },
            attachments = {

                Attachments(
                    profileTradeId = profileTradeId,
                    attachments = state.attachments,
                    onRemoveAttachment = { id -> state.eventSink(RemoveAttachment(id)) },
                )
            },
            notes = {

                Notes(
                    notes = state.notes,
                    onAddNote = { note -> state.eventSink(AddNote(note)) },
                    onUpdateNote = { id, note -> state.eventSink(UpdateNote(id, note)) },
                    onDeleteNote = { state.eventSink(DeleteNote(it)) },
                )
            },
        )
    }
}

@Composable
internal fun TradeScreen(
    details: @Composable () -> Unit,
    executions: @Composable () -> Unit,
    stopsAndTargets: @Composable () -> Unit,
    excursions: @Composable () -> Unit,
    tags: @Composable () -> Unit,
    attachments: @Composable () -> Unit,
    notes: @Composable () -> Unit,
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

                details()

                executions()

                stopsAndTargets()

                excursions()

                tags()

                attachments()

                notes()
            }
        }
    }
}
