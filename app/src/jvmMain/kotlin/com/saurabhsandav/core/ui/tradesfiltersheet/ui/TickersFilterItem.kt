package com.saurabhsandav.core.ui.tradesfiltersheet.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
                    modifier = Modifier.align(Alignment.CenterVertically),
                    tickerSuggestions = tickerSuggestions,
                    onAddTicker = onAddTicker,
                )
            },
        ) {

            selectedTickers.forEach { ticker ->

                key(ticker) {

                    ChipsSelectorSelectedItem(
                        modifier = Modifier.align(Alignment.CenterVertically),
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
    modifier: Modifier,
    tickerSuggestions: (String) -> Flow<List<String>>,
    onAddTicker: (String) -> Unit,
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
