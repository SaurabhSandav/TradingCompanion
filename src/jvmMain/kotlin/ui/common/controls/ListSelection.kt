package ui.common.controls

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
    val interactionSource = remember { MutableInteractionSource() }

    val modifier = if (!enabled) Modifier else Modifier.clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = { showSelectionDialog = true },
    )

    Box(modifier) {

        val defaultColors = TextFieldDefaults.outlinedTextFieldColors()

        OutlinedTextField(
            value = selection ?: placeholderText,
            onValueChange = {},
            label = label,
            readOnly = true,
            isError = isError,
            enabled = false,
            interactionSource = interactionSource,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                disabledTextColor = defaultColors.textColor(enabled).value,
                disabledBorderColor = defaultColors.indicatorColor(enabled, isError, interactionSource).value,
                disabledLabelColor = defaultColors.labelColor(enabled, isError, interactionSource).value,
                disabledPlaceholderColor = defaultColors.placeholderColor(enabled).value,
            ),
        )
    }

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

        Box(Modifier.fillMaxSize()) {

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

            Text(filterQuery, Modifier.align(Alignment.BottomStart))
        }
    }
}
