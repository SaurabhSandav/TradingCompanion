package com.saurabhsandav.core.ui.symbolselectiondialog

import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.saurabhsandav.core.LocalAppGraph
import com.saurabhsandav.core.ui.common.OutlinedTextBox
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.symbolselectiondialog.model.SymbolSelectionEvent.Filter
import com.saurabhsandav.core.ui.symbolselectiondialog.model.SymbolSelectionEvent.SymbolSelected
import com.saurabhsandav.trading.core.SymbolId

@Composable
fun SymbolSelectionField(
    type: SymbolSelectionType,
    selected: SymbolId?,
    onSelect: (SymbolId) -> Unit,
    supportingText: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
) {

    val scope = rememberCoroutineScope()
    val appGraph = LocalAppGraph.current
    val presenter = remember {
        appGraph.symbolSelectionGraphFactory
            .create()
            .presenterFactory
            .create(
                coroutineScope = scope,
                initialFilterQuery = "",
                initialSelectedSymbolId = selected,
            )
    }
    val state by presenter.state.collectAsState()

    var showSymbolSelectionDialog by state { false }

    OutlinedTextBox(
        value = state.selectedSymbol?.ticker ?: "",
        onClick = { showSymbolSelectionDialog = true },
        enabled = enabled,
        singleLine = true,
        label = { Text("Symbol") },
        placeholder = { Text("Select...") },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showSymbolSelectionDialog) },
        supportingText = supportingText,
        isError = isError,
        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
    )

    if (showSymbolSelectionDialog) {

        SymbolSelectionDialog(
            onDismissRequest = { showSymbolSelectionDialog = false },
            symbols = state.symbols,
            onSelect = { symbolId ->
                state.eventSink(SymbolSelected(symbolId))
                onSelect(symbolId)
            },
            type = type,
            onFilterChange = { query -> state.eventSink(Filter(query)) },
        )
    }
}
