package com.saurabhsandav.core.ui.trade.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.trades.model.TradeTagId
import com.saurabhsandav.core.ui.common.ConfirmationDialog
import com.saurabhsandav.core.ui.common.controls.ChipsSelectorAddButton
import com.saurabhsandav.core.ui.common.controls.ChipsSelectorBox
import com.saurabhsandav.core.ui.common.controls.ChipsSelectorSelectedItem
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.theme.dimens
import com.saurabhsandav.core.ui.trade.model.TradeState.TradeTag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

@Composable
internal fun Tags(
    tags: List<TradeTag>,
    tagSuggestions: (String) -> Flow<List<TradeTag>>,
    onAddTag: (TradeTagId) -> Unit,
    onRemoveTag: (TradeTagId) -> Unit,
) {

    Column(
        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {

        // Header
        Box(
            modifier = Modifier.height(MaterialTheme.dimens.listHeaderHeight).fillMaxWidth(),
            contentAlignment = Alignment.Center,
            content = { Text(text = "Tags") },
        )

        HorizontalDivider()

        ChipsSelectorBox(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.dimens.containerPadding),
            addButton = {

                AddTagButton(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    tagSuggestions = tagSuggestions,
                    onAddTag = onAddTag
                )
            },
        ) {

            tags.forEach { tag ->

                key(tag.id) {

                    var confirmRemove by state { false }

                    ChipsSelectorSelectedItem(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        name = tag.name,
                        description = tag.description,
                        onRemove = { confirmRemove = true },
                    )

                    if (confirmRemove) {

                        ConfirmationDialog(
                            text = "Remove tag \"${tag.name}\"?",
                            onDismiss = { confirmRemove = false },
                            onConfirm = {
                                onRemoveTag(tag.id)
                                confirmRemove = false
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddTagButton(
    modifier: Modifier,
    tagSuggestions: (String) -> Flow<List<TradeTag>>,
    onAddTag: (TradeTagId) -> Unit,
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
            onDismissRequest = { expanded = false }
        ) {

            var filter by state { "" }
            val filteredTags by remember {
                snapshotFlow { filter }.flatMapLatest(tagSuggestions)
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

            filteredTags.forEach { tag ->

                DropdownMenuItem(
                    text = { Text(tag.name) },
                    onClick = {
                        expanded = false
                        onAddTag(tag.id)
                    },
                )
            }
        }
    }
}
