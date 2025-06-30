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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import com.saurabhsandav.core.LocalAppModule
import com.saurabhsandav.core.ui.common.controls.LazyListSelectionDialog
import com.saurabhsandav.core.ui.symbolselectiondialog.SymbolSelectionType.Chart
import com.saurabhsandav.core.ui.symbolselectiondialog.SymbolSelectionType.Regular
import com.saurabhsandav.core.ui.symbolselectiondialog.model.SymbolSelectionEvent.Filter
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
    val presenter = remember { SymbolSelectionPresenter(scope, appModule.symbolsProvider) }
    val state by presenter.state.collectAsState()

    LazyListSelectionDialog(
        onDismissRequest = onDismissRequest,
        items = state.symbols,
        itemText = { it.value },
        onSelect = onSelect,
        onFilter = { query -> state.eventSink(Filter(query)) },
        title = { Text("Select Symbol") },
        onKeyEvent = onKeyEvent@{ keyEvent, symbol ->

            if (type !is Chart) return@onKeyEvent false

            val defaultCondition = keyEvent.isCtrlPressed && keyEvent.type == KeyEventType.KeyDown
            if (!defaultCondition) return@onKeyEvent false

            when (keyEvent.key) {
                Key.C if type.onOpenInCurrentWindow != null -> type.onOpenInCurrentWindow(symbol)
                Key.N -> type.onOpenInNewWindow(symbol)
                else -> return@onKeyEvent false
            }

            true
        },
        itemTrailingContent = (type as? Chart)?.let { type ->
            { symbol ->

                Row {

                    IconButton(
                        onClick = {
                            type.onOpenInNewWindow(symbol)
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
                                onOpenInCurrentWindow(symbol)
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
        initialFilterQuery = initialFilterQuery,
    )
}
