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
        Row(
            modifier = Modifier.height(64.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {

            Text(text = "Tags")
        }

        HorizontalDivider()

        ChipsSelectorBox(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.dimens.containerPadding),
            addButton = {

                AddTagButton(
                    tagSuggestions = tagSuggestions,
                    onAddTag = onAddTag
                )
            },
        ) {

            tags.forEach { tag ->

                key(tag.id) {

                    ChipsSelectorSelectedItem(
                        name = tag.name,
                        description = tag.description,
                        onRemove = { onRemoveTag(tag.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AddTagButton(
    tagSuggestions: (String) -> Flow<List<TradeTag>>,
    onAddTag: (TradeTagId) -> Unit,
) {

    var expanded by state { false }

    Box(
        modifier = Modifier.height(IntrinsicSize.Max),
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
