package com.saurabhsandav.core.ui.common.controls

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import com.saurabhsandav.core.ui.common.derivedState
import com.saurabhsandav.core.ui.common.state

@Composable
fun <T : Any> OutlinedListSelectionField(
    items: List<T>,
    itemText: (T) -> String,
    onSelection: (T) -> Unit,
    modifier: Modifier = Modifier,
    selection: T? = null,
    placeholderText: String = "Select...",
    label: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    trailingIcon: ListSelectionFieldDefaults.TrailingIcon<T>? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
) {

    var expanded by state { false }
    val selectionUpdated by rememberUpdatedState(selection)
    val selectedItemText by derivedState { selectionUpdated?.let(itemText) ?: "" }

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
            singleLine = true,
            label = label,
            placeholder = { Text(placeholderText) },
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
                items.filter { item -> itemText(item).contains(filter, ignoreCase = true) }
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
                    trailingIcon = trailingIcon?.run {
                        {
                            IconButton(
                                onClick = {
                                    expanded = false
                                    onClick(item)
                                }
                            ) {
                                Icon(icon, contentDescription = contentDescription)
                            }
                        }
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
fun <T : Any> ListSelectionField(
    items: List<T>,
    itemText: (T) -> String,
    onSelection: (T) -> Unit,
    modifier: Modifier = Modifier,
    selection: T? = null,
    placeholderText: String = "Select...",
    label: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    trailingIcon: ListSelectionFieldDefaults.TrailingIcon<T>? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
) {

    var expanded by state { false }
    val selectionUpdated by rememberUpdatedState(selection)
    val selectedItemText by derivedState { selectionUpdated?.let(itemText) ?: "" }

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
            singleLine = true,
            label = label,
            placeholder = { Text(placeholderText) },
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
                items.filter { item -> itemText(item).contains(filter, ignoreCase = true) }
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
                    trailingIcon = trailingIcon?.run {
                        {
                            IconButton(
                                onClick = {
                                    expanded = false
                                    onClick(item)
                                }
                            ) {
                                Icon(icon, contentDescription = contentDescription)
                            }
                        }
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

object ListSelectionFieldDefaults {

    data class TrailingIcon<T>(
        val icon: ImageVector,
        val contentDescription: String,
        val onClick: (T) -> Unit,
    )
}
