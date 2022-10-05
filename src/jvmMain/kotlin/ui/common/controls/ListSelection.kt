package ui.common.controls

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.ListItem
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ui.common.state

@Composable
fun ListSelectionField(
    items: List<String>,
    onSelection: (String) -> Unit,
    selection: String? = null,
    placeholderText: String = "Select...",
    label: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
) {

    var showSelectionDialog by state { false }

    OutlinedTextField(
        modifier = Modifier.onFocusChanged { if (it.hasFocus && enabled) showSelectionDialog = true },
        value = selection ?: placeholderText,
        onValueChange = {},
        label = label,
        readOnly = true,
        isError = isError,
        enabled = enabled,
    )

    if (showSelectionDialog) {

        ListSelectionDialog(
            items = items,
            onSelection = {
                onSelection(it)
                showSelectionDialog = false
            },
            selectionDialogTitle = placeholderText,
            onCloseRequest = { showSelectionDialog = false },
        )
    }
}

@Composable
private fun ListSelectionDialog(
    items: List<String>,
    onSelection: (ticker: String) -> Unit,
    selectionDialogTitle: String,
    onCloseRequest: () -> Unit,
) {

    Dialog(
        onCloseRequest = onCloseRequest,
        title = selectionDialogTitle,
    ) {

        var filterQuery by state { "" }
        val focusRequester = remember { FocusRequester() }
        val filteredItems by remember(items) {
            derivedStateOf { items.filter { it.startsWith(filterQuery, ignoreCase = true) } }
        }

        // For filtering list, Size is zero to hide
        BasicTextField(
            value = filterQuery,
            onValueChange = { value -> filterQuery = value.trim() },
            modifier = Modifier.size(0.dp, 0.dp).focusRequester(focusRequester)
        )

        SideEffect { focusRequester.requestFocus() }

        LazyColumn {

            items(
                items = filteredItems,
                key = { it },
            ) { itemText ->

                ListItem(
                    modifier = Modifier.clickable { onSelection(itemText) },
                ) {

                    val filterHighlightedText by remember(itemText) {
                        derivedStateOf {
                            buildAnnotatedString {

                                val filterQueryStartIndex = itemText.indexOf(filterQuery, ignoreCase = true)
                                val filterQueryEndIndex = filterQueryStartIndex + filterQuery.length
                                val filterQueryIndices = filterQueryStartIndex until filterQueryEndIndex

                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append(itemText.substring(filterQueryIndices))
                                }

                                append(itemText.removeRange(filterQueryIndices))
                            }
                        }
                    }

                    Text(
                        text = filterHighlightedText,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
