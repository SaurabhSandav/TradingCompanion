package com.saurabhsandav.core.ui.tradesfiltersheet.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.saurabhsandav.core.ui.common.controls.ChipsSelectorAddButton
import com.saurabhsandav.core.ui.common.controls.ChipsSelectorBox
import com.saurabhsandav.core.ui.common.controls.ChipsSelectorSelectedItem
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.theme.dimens
import com.saurabhsandav.trading.core.SymbolId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

@Composable
internal fun SymbolsFilterItem(
    selectedSymbols: List<SymbolId>,
    symbolSuggestions: (String) -> Flow<List<SymbolId>>,
    onAddSymbol: (SymbolId) -> Unit,
    onRemoveSymbol: (SymbolId) -> Unit,
) {

    TradeFilterItem(
        title = "Symbols",
        expandInitially = selectedSymbols.isNotEmpty(),
    ) {

        ChipsSelectorBox(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.dimens.containerPadding),
            addButton = {

                AddSymbolButton(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    symbolSuggestions = symbolSuggestions,
                    onAddSymbol = onAddSymbol,
                )
            },
        ) {

            selectedSymbols.forEach { symbolId ->

                key(symbolId) {

                    ChipsSelectorSelectedItem(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        name = symbolId.value,
                        onRemove = { onRemoveSymbol(symbolId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AddSymbolButton(
    modifier: Modifier,
    symbolSuggestions: (String) -> Flow<List<SymbolId>>,
    onAddSymbol: (SymbolId) -> Unit,
) {

    var expanded by state { false }

    Box(
        modifier = Modifier.height(IntrinsicSize.Max).then(modifier),
        contentAlignment = Alignment.Center,
    ) {

        ChipsSelectorAddButton(
            onAdd = { expanded = true },
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {

            val filter = rememberTextFieldState()
            val filteredSymbols by remember {
                snapshotFlow { filter.text.toString() }.flatMapLatest(symbolSuggestions)
            }.collectAsState(emptyList())
            val focusRequester = remember { FocusRequester() }

            OutlinedTextField(
                modifier = Modifier.focusRequester(focusRequester),
                state = filter,
                lineLimits = TextFieldLineLimits.SingleLine,
            )

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            filteredSymbols.forEach { symbolId ->

                DropdownMenuItem(
                    text = { Text(symbolId.value) },
                    onClick = {
                        expanded = false
                        onAddSymbol(symbolId)
                    },
                )
            }
        }
    }
}
