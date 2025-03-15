package com.saurabhsandav.core.ui.trade.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.trades.model.TradeTagId
import com.saurabhsandav.core.ui.common.ConfirmationDialog
import com.saurabhsandav.core.ui.common.SimpleTooltipBox
import com.saurabhsandav.core.ui.common.controls.ChipsSelectorBox
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.tags.model.TradeTag
import com.saurabhsandav.core.ui.tags.selector.TagSelectorDropdownMenu
import com.saurabhsandav.core.ui.tags.selector.TagSelectorType
import com.saurabhsandav.core.ui.theme.dimens
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId

@Composable
internal fun Tags(
    profileTradeId: ProfileTradeId,
    tags: List<TradeTag>,
    onAddTag: (TradeTagId) -> Unit,
    onRemoveTag: (TradeTagId) -> Unit,
    modifier: Modifier = Modifier,
) {

    val currentContentColor = LocalContentColor.current
    val currentTextStyle = LocalTextStyle.current

    TradeSection(
        modifier = modifier,
        title = "Tags",
        subtitle = when {
            tags.isEmpty() -> "No Tags"
            tags.size == 1 -> "1 Tag"
            else -> "${tags.size} Tags"
        },
        trailingContent = {

            // Wrap TagSelectorDropdownMenu with parent/default theme. Ignore ListItem `trailingContent` theme.
            AddTagButton(
                profileTradeId = profileTradeId,
                onAddTag = onAddTag,
                parentContentColor = currentContentColor,
                parentTextStyle = currentTextStyle,
            )
        },
    ) {

        val tagsPadding by animateDpAsState(if (tags.isEmpty()) 0.dp else MaterialTheme.dimens.containerPadding)

        ChipsSelectorBox(
            modifier = Modifier.fillMaxWidth().padding(tagsPadding),
        ) {

            tags.forEach { tag ->

                key(tag.id) {

                    TagChip(
                        tag = tag,
                        onRemoveTag = onRemoveTag,
                    )
                }
            }
        }
    }
}

@Composable
private fun FlowRowScope.TagChip(
    tag: TradeTag,
    onRemoveTag: (TradeTagId) -> Unit,
) {

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

@Composable
private fun AddTagButton(
    profileTradeId: ProfileTradeId,
    onAddTag: (TradeTagId) -> Unit,
    parentTextStyle: TextStyle,
    parentContentColor: Color,
) {

    var expanded by state { false }

    Box(
        modifier = Modifier.height(IntrinsicSize.Min),
        contentAlignment = Alignment.Center,
    ) {

        TradeSectionButton(
            onClick = { expanded = true },
            text = "Add Tag",
        )

        CompositionLocalProvider(
            LocalContentColor provides parentContentColor,
            LocalTextStyle provides parentTextStyle,
        ) {

            TagSelectorDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                profileId = profileTradeId.profileId,
                type = { TagSelectorType.ForTrades(listOf(profileTradeId.tradeId)) },
                onSelectTag = onAddTag,
            )
        }
    }
}
