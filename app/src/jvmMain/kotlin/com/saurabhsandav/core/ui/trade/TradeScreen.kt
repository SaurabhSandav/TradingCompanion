package com.saurabhsandav.core.ui.trade

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import com.saurabhsandav.core.LocalAppGraph
import com.saurabhsandav.core.ui.common.BoxWithScrollbar
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.app.rememberAppWindowState
import com.saurabhsandav.core.ui.common.thenIf
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
import com.saurabhsandav.core.ui.trade.ui.StopsList
import com.saurabhsandav.core.ui.trade.ui.Tags
import com.saurabhsandav.core.ui.trade.ui.TargetsList
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
            stops = {

                StopsList(
                    stops = state.stops,
                    stopPreviewer = state.stopPreviewer,
                    onAddStop = { state.eventSink(AddStop(it)) },
                    onDeleteStop = { state.eventSink(DeleteStop(it)) },
                    onSetPrimaryStop = { state.eventSink(SetPrimaryStop(it)) },
                )
            },
            targets = {

                TargetsList(
                    targets = state.targets,
                    showRValues = state.showTargetRValues,
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
    stops: @Composable () -> Unit,
    targets: @Composable () -> Unit,
    excursions: @Composable () -> Unit,
    tags: @Composable () -> Unit,
    attachments: @Composable () -> Unit,
    notes: @Composable () -> Unit,
) {

    Scaffold {

        BoxWithConstraints(Modifier.fillMaxSize()) {

            AdaptiveLayout(
                modifier = Modifier
                    .matchParentSize()
                    .padding(MaterialTheme.dimens.containerPadding),
                details = details,
                tags = tags,
                executions = executions,
                stops = stops,
                targets = targets,
                attachments = attachments,
                notes = notes,
                excursions = excursions,
            )
        }
    }
}

@Composable
private fun BoxWithConstraintsScope.AdaptiveLayout(
    modifier: Modifier,
    details: @Composable () -> Unit,
    executions: @Composable () -> Unit,
    stops: @Composable () -> Unit,
    targets: @Composable () -> Unit,
    tags: @Composable () -> Unit,
    attachments: @Composable () -> Unit,
    notes: @Composable () -> Unit,
    excursions: @Composable () -> Unit,
) {

    val is1Pane = maxWidth < 1000.dp
    val is2Pane = maxWidth >= 1000.dp && maxWidth < 1680.dp
    val is3Pane = maxWidth >= 1680.dp

    Row(
        modifier = modifier.matchParentSize(),
        horizontalArrangement = Arrangement.spacedBy(
            space = MaterialTheme.dimens.rowHorizontalSpacing,
            alignment = Alignment.CenterHorizontally,
        ),
    ) {

        val scrollState = rememberScrollState()

        BoxWithScrollbar(
            modifier = Modifier.fillMaxHeight().wrapContentHeight(),
            scrollbarAdapter = if (is2Pane || is3Pane) null else rememberScrollbarAdapter(scrollState),
        ) {

            Column(
                modifier = Modifier
                    .thenIf(
                        condition = is1Pane,
                        ifFalse = { width(400.dp) },
                        ifTrue = { matchParentSize() },
                    )
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.columnVerticalSpacing),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                details()
                tags()

                if (is1Pane) {
                    executions()
                    stops()
                    targets()
                    tags()
                    excursions()
                    attachments()
                    notes()
                }
            }
        }

        if (is2Pane || is3Pane) {

            VerticalDivider()

            val scrollStatePane2 = rememberScrollState()

            BoxWithScrollbar(
                modifier = Modifier
                    .thenIf(
                        condition = is2Pane,
                        ifFalse = { width(700.dp) },
                        ifTrue = { weight(1F) },
                    )
                    .fillMaxHeight(),
                scrollbarAdapter = rememberScrollbarAdapter(scrollStatePane2),
            ) {

                Column(
                    modifier = Modifier.verticalScroll(scrollStatePane2),
                    verticalArrangement = Arrangement.spacedBy(
                        space = MaterialTheme.dimens.columnVerticalSpacing,
                        alignment = Alignment.CenterVertically,
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {

                    executions()
                    stops()
                    targets()

                    if (is2Pane) {
                        excursions()
                        attachments()
                        notes()
                    }
                }
            }
        }

        if (is3Pane) {

            VerticalDivider()

            val scrollStatePane3 = rememberScrollState()

            BoxWithScrollbar(
                modifier = Modifier.weight(1F).fillMaxHeight(),
                scrollbarAdapter = rememberScrollbarAdapter(scrollStatePane3),
            ) {

                Column(
                    modifier = Modifier.verticalScroll(scrollStatePane3),
                    verticalArrangement = Arrangement.spacedBy(
                        space = MaterialTheme.dimens.columnVerticalSpacing,
                        alignment = Alignment.CenterVertically,
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    excursions()
                    attachments()
                    notes()
                }
            }
        }
    }
}
