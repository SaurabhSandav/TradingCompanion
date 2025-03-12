package com.saurabhsandav.core.ui.tradesfiltersheet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.model.TradeTagId
import com.saurabhsandav.core.ui.common.controls.ChipsSelectorAddButton
import com.saurabhsandav.core.ui.common.controls.ChipsSelectorBox
import com.saurabhsandav.core.ui.common.controls.ChipsSelectorSelectedItem
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.tags.model.TradeTag
import com.saurabhsandav.core.ui.tags.selector.TagSelectorDropdownMenu
import com.saurabhsandav.core.ui.tags.selector.TagSelectorType
import com.saurabhsandav.core.ui.theme.dimens

@Composable
internal fun TagsFilterItem(
    profileId: ProfileId,
    selectedTags: List<TradeTag>?,
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.rowHorizontalSpacing),
            ) {

                Text("Match")

                Spacer(Modifier.weight(1F))

                SingleChoiceSegmentedButtonRow {

                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        onClick = { onMatchAllTagsChange(false) },
                        selected = !matchAllTags,
                        label = { Text("Any") },
                    )

                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        onClick = { onMatchAllTagsChange(true) },
                        selected = matchAllTags,
                        label = { Text("All") },
                    )
                }
            }

            ChipsSelectorBox(
                modifier = Modifier.fillMaxWidth(),
                addButton = {

                    val updatedSelectedTags by rememberUpdatedState(selectedTags)

                    AddTagButton(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        profileId = profileId,
                        tagSelectorType = {
                            TagSelectorType.All(updatedSelectedTags?.map { it.id } ?: emptyList())
                        },
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
    profileId: ProfileId,
    tagSelectorType: () -> TagSelectorType,
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

        TagSelectorDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            profileId = profileId,
            type = tagSelectorType,
            onSelectTag = onAddTag,
            allowCreate = false,
        )
    }
}
