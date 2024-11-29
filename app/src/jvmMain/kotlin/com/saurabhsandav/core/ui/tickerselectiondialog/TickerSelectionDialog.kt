package com.saurabhsandav.core.ui.tickerselectiondialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import com.saurabhsandav.core.ui.common.app.AppDialogWindow
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.tickerselectiondialog.TickerSelectionType.Chart
import com.saurabhsandav.core.ui.tickerselectiondialog.TickerSelectionType.Regular
import com.saurabhsandav.core.ui.tickerselectiondialog.model.TickerSelectionEvent.Filter

@Composable
fun TickerSelectionDialog(
    onCloseRequest: () -> Unit,
    tickers: List<String>,
    onSelect: (String) -> Unit,
    type: TickerSelectionType = Regular,
) {

    val scope = rememberCoroutineScope()
    val presenter = remember { TickerSelectionPresenter(scope, tickers) }
    val state by presenter.state.collectAsState()

    AppDialogWindow(
        onCloseRequest = onCloseRequest,
        title = "Select Ticker...",
        onKeyEvent = {

            when (it.key) {
                Key.Escape -> {
                    onCloseRequest()
                    true
                }

                else -> false
            }
        }
    ) {

        Column {

            var filterQuery by state { "" }
            val focusRequester = remember { FocusRequester() }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                value = filterQuery,
                onValueChange = {
                    filterQuery = it
                    state.eventSink(Filter(it))
                },
                singleLine = true,
            )

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            LazyColumn(
                modifier = Modifier.weight(1F),
            ) {

                items(
                    items = state.tickers,
                    key = { it },
                ) { ticker ->

                    ListItem(
                        modifier = Modifier
                            .clickable {
                                onSelect(ticker)
                                onCloseRequest()
                            }
                            .animateItem(),
                        headlineContent = { Text(ticker) },
                        trailingContent = (type as? Chart)?.let { type ->
                            {

                                IconButton(
                                    onClick = {
                                        type.onOpenInNewTab(ticker)
                                        onCloseRequest()
                                    }
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Default.OpenInNew,
                                        contentDescription = "Open in new tab"
                                    )
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}
