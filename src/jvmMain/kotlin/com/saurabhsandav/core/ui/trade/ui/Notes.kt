package com.saurabhsandav.core.ui.trade.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.app.AppWindowOwner
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.trade.model.TradeState.TradeNote
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

        TextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                showAddWindow = true
                addWindowOwner.childrenToFront()
            },
            shape = RectangleShape,
            content = { Text("Add Note") },
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
            val initialFocusRequester = remember { FocusRequester() }

            LaunchedEffect(Unit) { initialFocusRequester.requestFocus() }

            TextField(
                modifier = Modifier.weight(1F).fillMaxWidth().focusRequester(initialFocusRequester),
                value = text,
                onValueChange = { text = it }
            )

            TextButton(
                modifier = Modifier.fillMaxWidth(),
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
    )
}
