package com.saurabhsandav.core.ui.tradesfiltersheet.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.saurabhsandav.core.trades.model.TradeTagId
import com.saurabhsandav.core.ui.common.SimpleTooltipBox
import com.saurabhsandav.core.ui.common.controls.ChipsSelectorAddButton
import com.saurabhsandav.core.ui.common.controls.ChipsSelectorBox
import com.saurabhsandav.core.ui.common.controls.ChipsSelectorSelectedItem
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.theme.dimens
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterState.TradeTag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

@Composable
internal fun TagsFilterItem(
    selectedTags: List<TradeTag>?,
    tagSuggestions: (String) -> Flow<List<TradeTag>>,
    onAddTag: (TradeTagId) -> Unit,
    onRemoveTag: (TradeTagId) -> Unit,
    matchAllTags: Boolean,
    onMatchAllTagsChange: (Boolean) -> Unit,
) {

    TradeFilterItem(
        title = "Tags",
        expandInitially = selectedTags != null,
    ) {

        Column(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.dimens.containerPadding),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.columnVerticalSpacing),
        ) {

            Row {

                Text("Match all tags")

                Spacer(Modifier.weight(1F))

                Switch(
                    checked = matchAllTags,
                    onCheckedChange = onMatchAllTagsChange,
                )
            }

            ChipsSelectorBox(
                modifier = Modifier.fillMaxWidth(),
                addButton = {

                    AddTagButton(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        tagSuggestions = tagSuggestions,
                        onAddTag = onAddTag,
                    )
                },
            ) {

                selectedTags?.forEach { tag ->

                    key(tag.id) {

                        ChipsSelectorSelectedItem(
                            modifier = Modifier.align(Alignment.CenterVertically),
                            name = tag.name,
                            description = tag.description,
                            onRemove = { onRemoveTag(tag.id) },
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
