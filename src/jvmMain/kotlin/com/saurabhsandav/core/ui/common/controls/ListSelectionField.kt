package com.saurabhsandav.core.ui.common.controls

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import com.saurabhsandav.core.ui.common.derivedState
import com.saurabhsandav.core.ui.common.state
import kotlinx.collections.immutable.ImmutableList

@Composable
fun <T : Any> OutlinedListSelectionField(
    items: ImmutableList<T>,
    itemText: (T) -> String,
    onSelection: (T) -> Unit,
    modifier: Modifier = Modifier,
    selection: T? = null,
    placeholderText: String = "Select...",
    label: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
) {

    var expanded by state { false }
    val selectionUpdated by rememberUpdatedState(selection)
    val selectedItemText by derivedState { selectionUpdated?.let(itemText) ?: placeholderText }

    ExposedDropdownMenuBox(
        modifier = modifier,
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
    ) {

        OutlinedTextField(
            modifier = Modifier.menuAnchor().onKeyEvent {
                when (it.key) {
                    Key.Enter, Key.NumPadEnter -> {
                        expanded = true
                        true
                    }

                    Key.Escape -> {
                        expanded = false
                        true
                    }

                    else -> false
                }
            },
            value = selectedItemText,
            onValueChange = {},
            enabled = enabled,
            readOnly = true,
            label = label,
            trailingIcon = { if (enabled) ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            supportingText = supportingText,
            isError = isError,
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {

            var filter by state { "" }
            val filteredItems by derivedState {
                items.filter { item -> itemText(item).lowercase().contains(filter.lowercase()) }
            }
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

            filteredItems.forEach { item ->

                DropdownMenuItem(
                    text = { Text(itemText(item)) },
                    onClick = {
                        expanded = false
                        onSelection(item)
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
fun <T : Any> ListSelectionField(
    items: ImmutableList<T>,
    itemText: (T) -> String,
    onSelection: (T) -> Unit,
    modifier: Modifier = Modifier,
    selection: T? = null,
    placeholderText: String = "Select...",
    label: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
) {

    var expanded by state { false }
    val selectionUpdated by rememberUpdatedState(selection)
    val selectedItemText by derivedState { selectionUpdated?.let(itemText) ?: placeholderText }

    ExposedDropdownMenuBox(
        modifier = modifier,
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
    ) {

        TextField(
            modifier = Modifier.menuAnchor().onKeyEvent {
                when (it.key) {
                    Key.Enter, Key.NumPadEnter -> {
                        expanded = true
                        true
                    }

                    Key.Escape -> {
                        expanded = false
                        true
                    }

                    else -> false
                }
            },
            value = selectedItemText,
            onValueChange = {},
            enabled = enabled,
            readOnly = true,
            label = label,
            trailingIcon = { if (enabled) ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            supportingText = supportingText,
            isError = isError,
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {

            var filter by state { "" }
            val filteredItems by derivedState {
                items.filter { item -> itemText(item).lowercase().contains(filter.lowercase()) }
            }
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

            filteredItems.forEach { item ->

                DropdownMenuItem(
                    text = { Text(itemText(item)) },
                    onClick = {
                        expanded = false
                        onSelection(item)
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
fun <T : Any> ListSelectionDialog(
    onDismissRequest: () -> Unit,
    items: ImmutableList<T>,
    itemText: (T) -> String,
    onSelection: (T) -> Unit,
    selection: T? = null,
    placeholderText: String = "Select...",
) {

    AlertDialog(
        onDismissRequest = onDismissRequest,
    ) {

        Surface {

            OutlinedListSelectionField(
                items = items,
                itemText = itemText,
                onSelection = onSelection,
                selection = selection,
                placeholderText = placeholderText,
            )
        }
    }
}
