package com.saurabhsandav.core.ui.tradesfiltersheet.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.saurabhsandav.core.ui.common.controls.ChipsSelectorAddButton
import com.saurabhsandav.core.ui.common.controls.ChipsSelectorBox
import com.saurabhsandav.core.ui.common.controls.ChipsSelectorSelectedItem
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.theme.dimens
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

@Composable
internal fun TickersFilterItem(
    selectedTickers: List<String>,
    tickerSuggestions: (String) -> Flow<List<String>>,
    onAddTicker: (String) -> Unit,
    onRemoveTicker: (String) -> Unit,
) {

    TradeFilterItem(
        title = "Tickers",
        expandInitially = selectedTickers.isNotEmpty(),
    ) {

        ChipsSelectorBox(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.dimens.containerPadding),
            addButton = {

                AddTickerButton(
                    tickerSuggestions = tickerSuggestions,
                    onAddTicker = onAddTicker,
                )
            },
        ) {

            selectedTickers.forEach { ticker ->

                key(ticker) {

                    ChipsSelectorSelectedItem(
                        name = ticker,
                        onRemove = { onRemoveTicker(ticker) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AddTickerButton(
    tickerSuggestions: (String) -> Flow<List<String>>,
    onAddTicker: (String) -> Unit,
) {

    var expanded by state { false }

    Box(
        modifier = Modifier.height(IntrinsicSize.Max),
        contentAlignment = Alignment.Center,
    ) {

        ChipsSelectorAddButton(
            onAdd = { expanded = true },
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {

            var filter by state { "" }
            val filteredTickers by remember {
                snapshotFlow { filter }.flatMapLatest(tickerSuggestions)
            }.collectAsState(emptyList())
            val focusRequester = remember { FocusRequester() }

            OutlinedTextField(
                modifier = Modifier.focusRequester(focusRequester),
                value = filter,
                onValueChange = { filter = it },
                singleLine = true,
            )

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            filteredTickers.forEach { ticker ->

                DropdownMenuItem(
                    text = { Text(ticker) },
                    onClick = {
                        expanded = false
                        onAddTicker(ticker)
                    },
                )
            }
        }
    }
}
