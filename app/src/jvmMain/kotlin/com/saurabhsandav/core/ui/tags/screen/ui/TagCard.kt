package com.saurabhsandav.core.ui.tags.screen.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.saurabhsandav.core.ui.common.DeleteConfirmationDialog
import com.saurabhsandav.core.ui.common.SimpleTooltipBox
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.tags.model.TradeTag
import com.saurabhsandav.core.ui.theme.dimens

@Composable
internal fun TagCard(
    tag: TradeTag,
    onNewTag: () -> Unit,
    onEditTag: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {

    var showDeleteConfirmationDialog by state { false }

    Card(modifier) {

        Column(
            modifier = Modifier
                .clickable(onClick = onEditTag)
                .padding(MaterialTheme.dimens.containerPadding),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.columnVerticalSpacing),
        ) {

            Row {

                Text(
                    modifier = Modifier.weight(1F),
                    text = tag.name,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge,
                )

                tag.color?.let {

                    // Use IconButton wrapper to align with 'Show options' menu
                    IconButton(
                        onClick = {},
                        enabled = false,
                        content = {
                            Box(Modifier.size(InputChipDefaults.IconSize).background(tag.color))
                        },
                    )
                }

                Box(
                    modifier = Modifier.height(IntrinsicSize.Max).then(modifier),
                    contentAlignment = Alignment.Center,
                ) {

                    var showOptions by state { false }

                    IconButton(
                        onClick = { showOptions = true },
                        content = {
                            Icon(Icons.Default.MoreVert, contentDescription = "Show options")
                        },
                    )

                    Options(
                        expanded = showOptions,
                        onDismissRequest = { showOptions = false },
                        onNew = onNewTag,
                        onEdit = onEditTag,
                        onDelete = { showDeleteConfirmationDialog = true },
                    )
                }
            }

            HorizontalDivider()

            var showTooltip by state { false }

            SimpleTooltipBox(tag.description?.takeIf { showTooltip }) {

                Text(
                    text = tag.description.orEmpty(),
                    maxLines = 3,
                    minLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { showTooltip = it.hasVisualOverflow },
                )
            }
        }
    }

    if (showDeleteConfirmationDialog) {

        DeleteConfirmationDialog(
            subject = "tag",
            onDismiss = { showDeleteConfirmationDialog = false },
            onConfirm = onDelete,
        )
    }
}

@Composable
private fun Options(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onNew: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {

        DropdownMenuItem(
            text = { Text("New") },
            onClick = {
                onDismissRequest()
                onNew()
            },
        )

        DropdownMenuItem(
            text = { Text("Edit") },
            onClick = {
                onDismissRequest()
                onEdit()
            },
        )

        DropdownMenuItem(
            text = { Text("Delete") },
            onClick = {
                onDismissRequest()
                onDelete()
            },
        )
    }
}
