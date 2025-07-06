package com.saurabhsandav.core.ui.symbolselectiondialog

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.paging.LoadState
import com.saurabhsandav.core.LocalAppModule
import com.saurabhsandav.core.ui.common.controls.ListSelectionDialog
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.symbolselectiondialog.SymbolSelectionType.Chart
import com.saurabhsandav.core.ui.symbolselectiondialog.SymbolSelectionType.Regular
import com.saurabhsandav.core.ui.symbolselectiondialog.model.SymbolSelectionEvent.Filter
import com.saurabhsandav.paging.compose.collectAsLazyPagingItems
import com.saurabhsandav.paging.compose.itemKey
import com.saurabhsandav.trading.core.SymbolId

@Composable
fun SymbolSelectionDialog(
    onDismissRequest: () -> Unit,
    onSelect: (SymbolId) -> Unit,
    type: SymbolSelectionType = Regular,
    initialFilterQuery: String = "",
) {

    val scope = rememberCoroutineScope()
    val appModule = LocalAppModule.current
    val presenter = remember {
        SymbolSelectionPresenter(
            coroutineScope = scope,
            initialFilterQuery = initialFilterQuery,
            symbolsProvider = appModule.symbolsProvider,
        )
    }
    val state by presenter.state.collectAsState()

    val items = state.symbols.collectAsLazyPagingItems()
    var selectedIndex by state { -1 }
    var filterQuery by state { state.filterQuery }

    ListSelectionDialog(
        onDismissRequest = onDismissRequest,
        itemCount = { items.itemCount },
        selectedIndex = selectedIndex,
        onSelectionChange = { index -> selectedIndex = index },
        onSelectionFinished = { index -> onSelect(items[index]!!) },
        key = items.itemKey { it },
        filterQuery = filterQuery,
        onFilterChange = { query ->
            state.eventSink(Filter(query))
            filterQuery = query
        },
        title = { Text("Select Symbol") },
        isLoading = items.loadState.refresh == LoadState.Loading,
        onKeyEvent = onKeyEvent@{ keyEvent, index ->

            if (type !is Chart) return@onKeyEvent false

            val defaultCondition = keyEvent.isCtrlPressed && keyEvent.type == KeyEventType.KeyDown
            if (!defaultCondition) return@onKeyEvent false

            when (keyEvent.key) {
                Key.C if type.onOpenInCurrentWindow != null -> type.onOpenInCurrentWindow(items[index]!!)
                Key.N -> type.onOpenInNewWindow(items[index]!!)
                else -> return@onKeyEvent false
            }

            onDismissRequest()

            true
        },
    ) { index ->

        val ticker = items[index]!!

        ListSelectionItem(
            isSelected = selectedIndex == index,
            onSelect = {
                onSelect(ticker)
                onDismissRequest()
            },
            headlineContent = { Text(ticker.value) },
            trailingContent = (type as? Chart)?.let { type ->
                {

                    Row {

                        IconButton(
                            onClick = {
                                type.onOpenInNewWindow(ticker)
                                onDismissRequest()
                            },
                        ) {
                            Icon(
                                Icons.Default.OpenInBrowser,
                                contentDescription = "Open in new window",
                            )
                        }

                        val onOpenInCurrentWindow = type.onOpenInCurrentWindow
                        if (onOpenInCurrentWindow != null) {

                            IconButton(
                                onClick = {
                                    onOpenInCurrentWindow(ticker)
                                    onDismissRequest()
                                },
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Default.OpenInNew,
                                    contentDescription = "Open in current Window",
                                )
                            }
                        }
                    }
                }
            },
        )
    }
}
