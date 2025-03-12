package com.saurabhsandav.core.ui.tradesfiltersheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.LocalScreensModule
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.model.TradeTagId
import com.saurabhsandav.core.ui.common.SideSheet
import com.saurabhsandav.core.ui.tags.model.TradeTag
import com.saurabhsandav.core.ui.theme.dimens
import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig
import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig.DateInterval
import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig.Notes
import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig.OpenClosed
import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig.PNL
import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig.Side
import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig.TimeInterval
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterEvent.AddTag
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterEvent.AddTicker
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterEvent.ApplyFilter
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterEvent.FilterByNetPnl
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterEvent.FilterDateInterval
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterEvent.FilterNotes
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterEvent.FilterOpenClosed
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterEvent.FilterPnl
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterEvent.FilterSide
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterEvent.FilterTimeInterval
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterEvent.RemoveTag
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterEvent.RemoveTicker
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterEvent.ResetFilter
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterEvent.SetFilter
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterEvent.SetMatchAllTagsEnabled
import com.saurabhsandav.core.ui.tradesfiltersheet.ui.DateIntervalFilterItem
import com.saurabhsandav.core.ui.tradesfiltersheet.ui.NotesFilterItem
import com.saurabhsandav.core.ui.tradesfiltersheet.ui.OpenClosedFilterItem
import com.saurabhsandav.core.ui.tradesfiltersheet.ui.PnlFilterItem
import com.saurabhsandav.core.ui.tradesfiltersheet.ui.SideFilterItem
import com.saurabhsandav.core.ui.tradesfiltersheet.ui.TagsFilterItem
import com.saurabhsandav.core.ui.tradesfiltersheet.ui.TickersFilterItem
import com.saurabhsandav.core.ui.tradesfiltersheet.ui.TimeIntervalFilterItem
import kotlinx.coroutines.flow.Flow

@Composable
internal fun TradesFilterSheet(
    profileId: ProfileId,
    filter: FilterConfig,
    onFilterChange: (FilterConfig) -> Unit,
) {

    val scope = rememberCoroutineScope()
    val screensModule = LocalScreensModule.current
    val presenter = remember { screensModule.tradesFilterModule(scope, profileId).presenter(filter, onFilterChange) }
    val state by presenter.state.collectAsState()

    LaunchedEffect(filter) {
        state.eventSink(SetFilter(filter))
    }

    TradesFilterSheet(
        profileId = profileId,
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
        onAddTag = { state.eventSink(AddTag(it)) },
        onRemoveTag = { state.eventSink(RemoveTag(it)) },
        matchAllTags = state.filterConfig.matchAllTags,
        onMatchAllTagsChange = { state.eventSink(SetMatchAllTagsEnabled(it)) },
        selectedTickers = state.filterConfig.tickers,
        tickerSuggestions = state.tickerSuggestions,
        onAddTicker = { state.eventSink(AddTicker(it)) },
        onRemoveTicker = { state.eventSink(RemoveTicker(it)) },
        onReset = { state.eventSink(ResetFilter) },
        onApply = { state.eventSink(ApplyFilter) },
    )
}

@Composable
private fun TradesFilterSheet(
    profileId: ProfileId,
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
    selectedTags: List<TradeTag>?,
    onAddTag: (TradeTagId) -> Unit,
    onRemoveTag: (TradeTagId) -> Unit,
    matchAllTags: Boolean,
    onMatchAllTagsChange: (Boolean) -> Unit,
    selectedTickers: List<String>,
    tickerSuggestions: (String) -> Flow<List<String>>,
    onAddTicker: (String) -> Unit,
    onRemoveTicker: (String) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
) {

    SideSheet {

        Column(Modifier.width(400.dp).verticalScroll(rememberScrollState())) {

            OpenClosedFilterItem(openClosed, onOpenClosedChange)

            HorizontalDivider()

            SideFilterItem(side, onSideChange)

            HorizontalDivider()

            DateIntervalFilterItem(dateInterval, onDateIntervalChange)

            HorizontalDivider()

            TimeIntervalFilterItem(timeInterval, onTimeIntervalChange)

            HorizontalDivider()

            PnlFilterItem(pnl, onPnlChange, filterByNetPnl, onFilterByNetPnlChange)

            HorizontalDivider()

            NotesFilterItem(notes, onNotesChange)

            HorizontalDivider()

            TagsFilterItem(
                profileId = profileId,
                selectedTags = selectedTags,
                onAddTag = onAddTag,
                onRemoveTag = onRemoveTag,
                matchAllTags = matchAllTags,
                onMatchAllTagsChange = onMatchAllTagsChange,
            )

            HorizontalDivider()

            TickersFilterItem(
                selectedTickers = selectedTickers,
                tickerSuggestions = tickerSuggestions,
                onAddTicker = onAddTicker,
                onRemoveTicker = onRemoveTicker,
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
        modifier = Modifier.fillMaxWidth().padding(MaterialTheme.dimens.containerPadding),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.rowHorizontalSpacing),
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
