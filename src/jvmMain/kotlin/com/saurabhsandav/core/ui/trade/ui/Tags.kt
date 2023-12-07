package com.saurabhsandav.core.ui.trade.ui

import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.trades.model.TradeTagId
import com.saurabhsandav.core.ui.common.IconButtonWithTooltip
import com.saurabhsandav.core.ui.common.Tooltip
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.trade.model.TradeState.TradeTag
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

@Composable
internal fun Tags(
    tags: ImmutableList<TradeTag>,
    tagSuggestions: (String) -> Flow<ImmutableList<TradeTag>>,
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

        Divider()

        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.Center,
        ) {

            tags.forEach { tag ->

                key(tag.id) {

                    TooltipArea(
                        tooltip = { Tooltip(tag.description) },
                    ) {

                        InputChip(
                            selected = false,
                            onClick = {},
                            label = { Text(tag.name) },
                            trailingIcon = {

                                IconButtonWithTooltip(
                                    onClick = { onRemoveTag(tag.id) },
                                    tooltipText = "Delete Tag",
                                ) {

                                    Icon(
                                        modifier = Modifier.size(InputChipDefaults.IconSize),
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Delete Tag",
                                    )
                                }
                            },
                        )
                    }
                }
            }

            AddTagButton(
                tagSuggestions = tagSuggestions,
                onAddTag = onAddTag,
            )
        }
    }
}

@Composable
private fun AddTagButton(
    tagSuggestions: (String) -> Flow<ImmutableList<TradeTag>>,
    onAddTag: (TradeTagId) -> Unit,
) {

    var expanded by state { false }

    Box(
        modifier = Modifier.height(IntrinsicSize.Max),
        contentAlignment = Alignment.Center,
    ) {

        IconButtonWithTooltip(
            onClick = { expanded = true },
            tooltipText = "Add Tag",
            content = {
                Icon(Icons.Default.Add, contentDescription = "Add Tag")
            },
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {

            var filter by state { "" }
            val filteredTags by remember {
                snapshotFlow { filter }.flatMapLatest(tagSuggestions)
            }.collectAsState(persistentListOf())
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
