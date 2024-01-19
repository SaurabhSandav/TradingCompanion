package com.saurabhsandav.core.ui.tradesfiltersheet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.LocalAppModule
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.model.TradeTagId
import com.saurabhsandav.core.ui.common.SideSheet
import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig
import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig.*
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterEvent.*
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterState.TradeTag
import com.saurabhsandav.core.ui.tradesfiltersheet.ui.*
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow

@Composable
internal fun TradesFilterSheet(
    profileId: ProfileId,
    filter: FilterConfig,
    onFilterChange: (FilterConfig) -> Unit,
) {

    val scope = rememberCoroutineScope()
    val appModule = LocalAppModule.current
    val presenter = remember { appModule.tradesFilterModule(scope, profileId).presenter(filter, onFilterChange) }
    val state by presenter.state.collectAsState()

    LaunchedEffect(filter) {
        state.eventSink(SetFilter(filter))
    }

    TradesFilterSheet(
        openClosed = state.filterConfig.openClosed,
        onOpenClosedChange = { state.eventSink(FilterOpenClosed(it)) },
        side = state.filterConfig.side,
        onSideChange = { state.eventSink(FilterSide(it)) },
        dateInterval = state.filterConfig.dateInterval,
        onDateIntervalChange = { state.eventSink(FilterDateInterval(it)) },
        timeInterval = state.filterConfig.timeInterval,
        onTimeIntervalChange = { state.eventSink(FilterTimeInterval(it)) },
        pnl = state.filterConfig.pnl,
        onPnlChange = { state.eventSink(FilterPnl(it)) },
        filterByNetPnl = state.filterConfig.filterByNetPnl,
        onFilterByNetPnlChange = { state.eventSink(FilterByNetPnl(it)) },
        notes = state.filterConfig.notes,
        onNotesChange = { state.eventSink(FilterNotes(it)) },
        selectedTags = state.selectedTags,
        tagSuggestions = state.tagSuggestions,
        onAddTag = { state.eventSink(AddTag(it)) },
        onRemoveTag = { state.eventSink(RemoveTag(it)) },
        matchAllTags = state.filterConfig.matchAllTags,
        onMatchAllTagsChange = { state.eventSink(SetMatchAllTagsEnabled(it)) },
        onReset = { state.eventSink(ResetFilter) },
        onApply = { state.eventSink(ApplyFilter) },
    )
}

@Composable
private fun TradesFilterSheet(
    openClosed: OpenClosed,
    onOpenClosedChange: (OpenClosed) -> Unit,
    side: Side,
    onSideChange: (Side) -> Unit,
    dateInterval: DateInterval,
    onDateIntervalChange: (DateInterval) -> Unit,
    timeInterval: TimeInterval,
    onTimeIntervalChange: (TimeInterval) -> Unit,
    pnl: PNL,
    onPnlChange: (PNL) -> Unit,
    filterByNetPnl: Boolean,
    onFilterByNetPnlChange: (Boolean) -> Unit,
    notes: Notes,
    onNotesChange: (Notes) -> Unit,
    selectedTags: ImmutableList<TradeTag>?,
    tagSuggestions: (String) -> Flow<ImmutableList<TradeTag>>,
    onAddTag: (TradeTagId) -> Unit,
    onRemoveTag: (TradeTagId) -> Unit,
    matchAllTags: Boolean,
    onMatchAllTagsChange: (Boolean) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
) {

    SideSheet {

        Column(Modifier.width(400.dp).verticalScroll(rememberScrollState())) {

            OpenClosedFilterItem(openClosed, onOpenClosedChange)

            Divider()

            SideFilterItem(side, onSideChange)

            Divider()

            DateIntervalFilterItem(dateInterval, onDateIntervalChange)

            Divider()

            TimeIntervalFilterItem(timeInterval, onTimeIntervalChange)

            Divider()

            PnlFilterItem(pnl, onPnlChange, filterByNetPnl, onFilterByNetPnlChange)

            Divider()

            NotesFilterItem(notes, onNotesChange)

            Divider()

            TagsFilterItem(
                selectedTags = selectedTags,
                tagSuggestions = tagSuggestions,
                onAddTag = onAddTag,
                onRemoveTag = onRemoveTag,
                matchAllTags = matchAllTags,
                onMatchAllTagsChange = onMatchAllTagsChange,
            )

            Spacer(Modifier.weight(1F))

            Buttons(
                onReset = onReset,
                onApply = onApply,
            )
        }
    }
}

@Composable
private fun Buttons(
    onReset: () -> Unit,
    onApply: () -> Unit,
) {

    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {

        OutlinedButton(
            modifier = Modifier.weight(1F),
            onClick = onReset,
            shape = MaterialTheme.shapes.small,
            content = { Text("Reset") },
        )

        OutlinedButton(
            modifier = Modifier.weight(1F),
            onClick = onApply,
            shape = MaterialTheme.shapes.small,
            content = { Text("Apply") },
        )
    }
}
