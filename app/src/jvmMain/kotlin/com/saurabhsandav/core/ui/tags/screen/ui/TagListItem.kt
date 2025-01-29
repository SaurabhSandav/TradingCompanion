package com.saurabhsandav.core.ui.tags.screen.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NewLabel
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.ui.common.DeleteConfirmationDialog
import com.saurabhsandav.core.ui.common.IconButtonWithTooltip
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.tags.screen.model.TagsScreenState.Tag
import com.saurabhsandav.core.ui.theme.dimens

@Composable
internal fun TagListItem(
    tag: Tag,
    onNewTag: () -> Unit,
    onEditTag: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {

    var showDeleteConfirmationDialog by state { false }

    ListItem(
        modifier = Modifier.clickable(onClick = onEditTag).then(modifier),
        headlineContent = {

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
        supportingContent = tag.description?.let { { Text(it) } },
        trailingContent = {

            Row {

                IconButtonWithTooltip(
                    onClick = onNewTag,
                    tooltipText = "New",
                    content = {
                        Icon(Icons.Default.NewLabel, contentDescription = "New")
                    },
                )

                IconButtonWithTooltip(
                    onClick = onEditTag,
                    tooltipText = "Edit",
                    content = {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    },
                )

                IconButtonWithTooltip(
                    onClick = { showDeleteConfirmationDialog = true },
                    tooltipText = "Delete",
                    content = {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    },
                )
            }
        },
    )

    HorizontalDivider()

    if (showDeleteConfirmationDialog) {

        DeleteConfirmationDialog(
            subject = "tag",
            onDismiss = { showDeleteConfirmationDialog = false },
            onConfirm = onDelete,
        )
    }
}
