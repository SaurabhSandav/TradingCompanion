package com.saurabhsandav.core.ui.trade.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.halilibo.richtext.markdown.Markdown
import com.halilibo.richtext.ui.RichText
import com.saurabhsandav.core.trades.model.TradeNoteId
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.app.AppWindowManager
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.trade.model.TradeState.TradeNote
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun Notes(
    notes: ImmutableList<TradeNote>,
    onAddNote: (note: String, isMarkdown: Boolean) -> Unit,
    onUpdateNote: (id: TradeNoteId, note: String, isMarkdown: Boolean) -> Unit,
    onDeleteNote: (id: TradeNoteId) -> Unit,
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
                        headlineContent = {

                            when {
                                note.isMarkdown -> RichText { Markdown(note.noteText) }
                                else -> Text(note.noteText)
                            }
                        },
                    )

                    if (showEditDialog) {

                        NoteEditorWindow(
                            noteId = note.id,
                            noteText = note.noteText,
                            isMarkdown = note.isMarkdown,
                            onSaveNote = { noteText, isMarkdown -> onUpdateNote(note.id, noteText, isMarkdown) },
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

        val addWindowManager = remember { AppWindowManager() }

        TextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = addWindowManager::openWindow,
            shape = RectangleShape,
            content = { Text("Add Note") },
        )

        addWindowManager.Window {

            NoteEditorWindow(
                onCloseRequest = addWindowManager::closeWindow,
                onSaveNote = onAddNote,
            )
        }
    }
}

@Composable
private fun NoteEditorWindow(
    onSaveNote: (note: String, isMarkdown: Boolean) -> Unit,
    onCloseRequest: () -> Unit,
    noteId: TradeNoteId? = null,
    noteText: String = "",
    isMarkdown: Boolean = false,
) {

    AppWindow(
        onCloseRequest = onCloseRequest,
        title = if (noteId != null) "Edit note ($noteId)" else "Add note",
    ) {

        Column(Modifier.fillMaxSize()) {

            var text by state { noteText }
            var previewMarkdown by state { false }
            var isMarkdownEdit by state { isMarkdown }
            val initialFocusRequester = remember { FocusRequester() }

            LaunchedEffect(Unit) { initialFocusRequester.requestFocus() }

            Box(Modifier.weight(1F).fillMaxWidth()) {

                Crossfade(previewMarkdown) { previewMarkdown ->

                    when {
                        previewMarkdown -> {

                            val scrollState = rememberScrollState()

                            RichText(
                                modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp),
                                children = { Markdown(text) }
                            )

                            VerticalScrollbar(
                                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                                adapter = rememberScrollbarAdapter(scrollState),
                            )
                        }

                        else -> TextField(
                            modifier = Modifier.fillMaxSize().focusRequester(initialFocusRequester),
                            value = text,
                            onValueChange = { text = it }
                        )
                    }
                }
            }

            Divider()

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {

                AnimatedVisibility(isMarkdownEdit) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {

                        Text("Preview Markdown")

                        Switch(
                            checked = previewMarkdown,
                            onCheckedChange = { previewMarkdown = it },
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {

                    Text("Markdown")

                    Switch(
                        checked = isMarkdownEdit,
                        onCheckedChange = { isMarkdownEdit = it },
                    )
                }
            }

            Divider()

            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onCloseRequest()
                    onSaveNote(text, isMarkdownEdit)
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
