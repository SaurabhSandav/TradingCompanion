package com.saurabhsandav.core.ui.trades.detail.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.AlertDialog
import androidx.compose.material.SnackbarDefaults
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.app.AppWindowOwner
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.trades.detail.model.TradeDetailState.TradeNote
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun Notes(
    notes: ImmutableList<TradeNote>,
    onAddNote: (note: String) -> Unit,
    onUpdateNote: (id: Long, note: String) -> Unit,
    onDeleteNote: (id: Long) -> Unit,
) {

    Column(
        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        // Header
        Row(
            modifier = Modifier.height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {

            Text(text = "Notes")
        }

        Divider()

        notes.forEach { note ->

            key(note.id) {

                var showEditDialog by state { false }
                var showDeleteConfirmationDialog by state { false }

                ContextMenuArea(items = {
                    listOf(
                        ContextMenuItem("Edit") { showEditDialog = true },
                        ContextMenuItem("Delete") { showDeleteConfirmationDialog = true }
                    )
                }) {

                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        overlineContent = { Text(note.dateText) },
                        headlineContent = { Text(note.note) },
                    )

                    if (showEditDialog) {

                        NoteEditorWindow(
                            noteId = note.id,
                            note = note.note,
                            onSaveNote = { onUpdateNote(note.id, it) },
                            onCloseRequest = { showEditDialog = false },
                        )
                    }

                    if (showDeleteConfirmationDialog) {

                        DeleteNoteConfirmationDialog(
                            onDismiss = { showDeleteConfirmationDialog = false },
                            onConfirm = { onDeleteNote(note.id) },
                        )
                    }
                }
            }

            Divider()
        }

        var showAddWindow by state { false }
        val addWindowOwner = remember { AppWindowOwner() }

        Button(
            onClick = {
                showAddWindow = true
                addWindowOwner.childrenToFront()
            },
            content = { Text("Add note") },
        )

        if (showAddWindow) {

            AppWindowOwner(addWindowOwner) {

                NoteEditorWindow(
                    onCloseRequest = { showAddWindow = false },
                    onSaveNote = onAddNote,
                )
            }
        }
    }
}

@Composable
private fun NoteEditorWindow(
    onSaveNote: (String) -> Unit,
    onCloseRequest: () -> Unit,
    noteId: Long? = null,
    note: String = "",
) {

    AppWindow(
        onCloseRequest = onCloseRequest,
        title = if (noteId != null) "Edit note ($noteId)" else "Add note",
    ) {

        Column(Modifier.fillMaxSize()) {

            var text by state { note }

            TextField(
                modifier = Modifier.weight(1F).fillMaxWidth(),
                value = text,
                onValueChange = { text = it }
            )

            Button(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = {
                    onCloseRequest()
                    onSaveNote(text)
                },
                content = { Text("Save") },
            )
        }
    }
}

@Composable
private fun DeleteNoteConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {

    AlertDialog(
        modifier = Modifier.width(300.dp),
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Yes")
            }
        },
        text = {
            Text("Are you sure you want to delete the note?")
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("No")
            }
        },
        shape = MaterialTheme.shapes.medium,
        backgroundColor = MaterialTheme.colorScheme.surface,
        contentColor = contentColorFor(SnackbarDefaults.backgroundColor),
    )
}
