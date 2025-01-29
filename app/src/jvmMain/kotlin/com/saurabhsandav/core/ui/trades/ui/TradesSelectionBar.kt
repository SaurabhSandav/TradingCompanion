package com.saurabhsandav.core.ui.trades.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.model.TradeId
import com.saurabhsandav.core.trades.model.TradeTagId
import com.saurabhsandav.core.ui.attachmentform.AttachmentFormWindow
import com.saurabhsandav.core.ui.attachmentform.model.AttachmentFormType
import com.saurabhsandav.core.ui.common.*
import com.saurabhsandav.core.ui.tags.model.TradeTag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

@Composable
internal fun TradesSelectionBar(
    profileId: ProfileId,
    selectionManager: SelectionManager<TradeId>,
    onDeleteTrades: (List<TradeId>) -> Unit,
    tagSuggestions: (String) -> Flow<List<TradeTag>>,
    onAddTag: (List<TradeId>, TradeTagId) -> Unit,
    onOpenChart: (List<TradeId>) -> Unit,
) {

    SelectionBar(selectionManager) {

        var showDeleteConfirmationDialog by state { false }

        Item(
            onClick = { showDeleteConfirmationDialog = true },
            text = "Delete",
        )

        var showAddAttachmentDialog by state { false }

        Item(
            onClick = { showAddAttachmentDialog = true },
            text = "Add Attachment",
        )

        var expanded by state { false }

        AddTagContainer(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            tagSuggestions = tagSuggestions,
            onAddTag = { tagId ->
                onAddTag(selectionManager.selection.toList(), tagId)
                selectionManager.clear()
            },
        ) {

            Item(
                onClick = { expanded = true },
                text = "Add Tag",
            )
        }

        Item(
            onClick = {
                onOpenChart(selectionManager.selection.toList())
                selectionManager.clear()
            },
            text = "Chart",
        )

        if (showDeleteConfirmationDialog) {

            DeleteConfirmationDialog(
                subject = "trades",
                onDismiss = { showDeleteConfirmationDialog = false },
                onConfirm = {
                    onDeleteTrades(selectionManager.selection.toList())
                    selectionManager.clear()
                },
            )
        }

        if (showAddAttachmentDialog) {

            AttachmentFormWindow(
                profileId = profileId,
                formType = remember { AttachmentFormType.New(selectionManager.selection.toList()) },
                onCloseRequest = {
                    showAddAttachmentDialog = false
                    selectionManager.clear()
                },
            )
        }
    }
}

@Composable
private fun AddTagContainer(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    tagSuggestions: (String) -> Flow<List<TradeTag>>,
    onAddTag: (TradeTagId) -> Unit,
    item: @Composable () -> Unit,
) {

    Box(
        modifier = Modifier.wrapContentSize(Alignment.TopCenter),
        contentAlignment = Alignment.Center,
        propagateMinConstraints = true,
    ) {

        item()

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
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
                            onDismissRequest()
                            onAddTag(tag.id)
                        },
                    )
                }
            }
        }
    }
}
