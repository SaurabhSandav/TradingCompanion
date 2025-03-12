package com.saurabhsandav.core.ui.common.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import com.saurabhsandav.core.ui.common.derivedState
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.theme.keyboardSelectionBackgroundColor

@Composable
fun <T : Any> OutlinedListSelectionField(
    items: List<T>,
    itemText: (T) -> String,
    onSelect: (T) -> Unit,
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
    var navigationSelectedIndex by state { -1 }

    ExposedDropdownMenuBox(
        modifier = modifier,
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
    ) {

        OutlinedTextField(
            modifier = Modifier
                .menuAnchor(
                    type = MenuAnchorType.PrimaryNotEditable,
                    enabled = enabled,
                )
                .onKeyEvent { keyEvent ->

                    if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false

                    expanded = when (keyEvent.key) {
                        Key.Enter, Key.NumPadEnter -> true
                        Key.Escape -> false
                        else -> return@onKeyEvent false
                    }

                    return@onKeyEvent true
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
            onDismissRequest = { expanded = false },
        ) {

            var filterQuery by state { "" }
            val filteredItems by derivedState {
                items.filter { item -> itemText(item).contains(filterQuery, ignoreCase = true) }
            }
            val focusRequester = remember { FocusRequester() }

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent { keyEvent ->

                        if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                        when (keyEvent.key) {
                            Key.DirectionUp -> navigationSelectedIndex = (navigationSelectedIndex - 1).coerceAtLeast(0)
                            Key.DirectionDown ->
                                navigationSelectedIndex =
                                    (navigationSelectedIndex + 1).coerceAtMost(filteredItems.lastIndex)

                            Key.Enter -> {
                                onSelect(filteredItems[navigationSelectedIndex])
                                expanded = false
                            }

                            else -> return@onPreviewKeyEvent false
                        }

                        true
                    },
                value = filterQuery,
                onValueChange = {
                    navigationSelectedIndex = -1
                    filterQuery = it
                },
                singleLine = true,
            )

            filteredItems.forEachIndexed { index, item ->

                key(item) {

                    val isSelected = navigationSelectedIndex == index
                    val bringIntoViewRequester = remember { BringIntoViewRequester() }

                    // Scroll item into view if selected
                    LaunchedEffect(isSelected) {
                        if (isSelected) bringIntoViewRequester.bringIntoView()
                    }

                    DropdownMenuItem(
                        modifier = Modifier
                            .bringIntoViewRequester(bringIntoViewRequester)
                            .background(
                                color = when {
                                    navigationSelectedIndex == index -> keyboardSelectionBackgroundColor()
                                    else -> Color.Unspecified
                                },
                            ),
                        text = { Text(itemText(item)) },
                        onClick = {
                            expanded = false
                            onSelect(item)
                        },
                        trailingIcon = trailingIcon?.run {
                            {
                                IconButton(
                                    onClick = {
                                        expanded = false
                                        onClick(item)
                                    },
                                ) {
                                    Icon(icon, contentDescription = contentDescription)
                                }
                            }
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }

                if (index != filteredItems.lastIndex) HorizontalDivider()
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
