package com.saurabhsandav.core.ui.trade.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.trades.model.TradeTagId
import com.saurabhsandav.core.ui.common.ConfirmationDialog
import com.saurabhsandav.core.ui.common.SimpleTooltipBox
import com.saurabhsandav.core.ui.common.controls.ChipsSelectorAddButton
import com.saurabhsandav.core.ui.common.controls.ChipsSelectorBox
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.tags.model.TradeTag
import com.saurabhsandav.core.ui.theme.dimens
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
        Text(
            modifier = Modifier
                .height(MaterialTheme.dimens.listHeaderHeight)
                .fillMaxWidth()
                .wrapContentSize(),
            text = "Tags",
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

                    SimpleTooltipBox(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        tooltipText = tag.description,
                    ) {

                        InputChip(
                            selected = false,
                            onClick = { confirmRemove = true },
                            label = {

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.rowHorizontalSpacing),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {

                                    Text(tag.name)

                                    tag.color?.let {
                                        Box(Modifier.size(InputChipDefaults.IconSize).background(tag.color))
                                    }
                                }
                            },
                            trailingIcon = {

                                Icon(
                                    modifier = Modifier.size(InputChipDefaults.IconSize),
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                )
                            },
                        )
                    }

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

                SimpleTooltipBox(tag.description) {

                    DropdownMenuItem(
                        text = { Text(tag.name) },
                        trailingIcon = tag.color?.let {
                            { Box(Modifier.size(InputChipDefaults.IconSize).background(tag.color)) }
                        },
                        onClick = {
                            expanded = false
                            onAddTag(tag.id)
                        },
                    )
                }
            }
        }
    }
}
