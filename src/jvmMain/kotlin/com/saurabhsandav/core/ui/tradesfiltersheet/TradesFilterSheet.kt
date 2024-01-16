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
import com.saurabhsandav.core.ui.common.SideSheet
import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig
import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig.*
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterEvent.*
import com.saurabhsandav.core.ui.tradesfiltersheet.ui.NotesFilterItem
import com.saurabhsandav.core.ui.tradesfiltersheet.ui.OpenClosedFilterItem
import com.saurabhsandav.core.ui.tradesfiltersheet.ui.PnlFilterItem
import com.saurabhsandav.core.ui.tradesfiltersheet.ui.SideFilterItem

@Composable
internal fun TradesFilterSheet(
    filter: FilterConfig,
    onFilterChange: (FilterConfig) -> Unit,
) {

    val scope = rememberCoroutineScope()
    val appModule = LocalAppModule.current
    val presenter = remember { appModule.tradesFilterModule(scope).presenter(filter, onFilterChange) }
    val state by presenter.state.collectAsState()

    LaunchedEffect(filter) {
        state.eventSink(SetFilter(filter))
    }

    TradesFilterSheet(
        openClosed = state.filterConfig.openClosed,
        onOpenClosedChange = { state.eventSink(FilterOpenClosed(it)) },
        side = state.filterConfig.side,
        onSideChange = { state.eventSink(FilterSide(it)) },
        pnl = state.filterConfig.pnl,
        onPnlChange = { state.eventSink(FilterPnl(it)) },
        filterByNetPnl = state.filterConfig.filterByNetPnl,
        onFilterByNetPnlChange = { state.eventSink(FilterByNetPnl(it)) },
        notes = state.filterConfig.notes,
        onNotesChange = { state.eventSink(FilterNotes(it)) },
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
    pnl: PNL,
    onPnlChange: (PNL) -> Unit,
    filterByNetPnl: Boolean,
    onFilterByNetPnlChange: (Boolean) -> Unit,
    notes: Notes,
    onNotesChange: (Notes) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
) {

    SideSheet {

        Column(Modifier.width(400.dp).verticalScroll(rememberScrollState())) {

            OpenClosedFilterItem(openClosed, onOpenClosedChange)

            Divider()

            SideFilterItem(side, onSideChange)

            Divider()

            PnlFilterItem(pnl, onPnlChange, filterByNetPnl, onFilterByNetPnlChange)

            Divider()

            NotesFilterItem(notes, onNotesChange)

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
