package com.saurabhsandav.core.ui.trade.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.window.WindowPlacement
import com.mikepenz.markdown.m3.Markdown
import com.saurabhsandav.core.ui.common.DeleteConfirmationDialog
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.app.AppWindowManager
import com.saurabhsandav.core.ui.common.app.rememberAppWindowState
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.theme.dimens
import com.saurabhsandav.core.ui.trade.model.TradeState.TradeNote
import com.saurabhsandav.trading.record.model.TradeNoteId

@Composable
internal fun Notes(
    notes: List<TradeNote>,
    onAddNote: (note: String) -> Unit,
    onUpdateNote: (id: TradeNoteId, note: String) -> Unit,
    onDeleteNote: (id: TradeNoteId) -> Unit,
    modifier: Modifier = Modifier,
) {

    val addWindowManager = remember { AppWindowManager() }

    TradeSection(
        modifier = modifier,
        title = "Notes",
        subtitle = when {
            notes.isEmpty() -> "No Notes"
            notes.size == 1 -> "1 Note"
            else -> "${notes.size} Notes"
        },
        trailingContent = {

            TradeSectionButton(
                onClick = addWindowManager::openWindow,
                text = "Add Note",
            )
        },
    ) {

        notes.forEach { note ->

            key(note.id) {

                var showEditDialog by state { false }
                var showDeleteConfirmationDialog by state { false }

                ContextMenuArea(
                    items = {
                        listOf(
                            ContextMenuItem("Edit") { showEditDialog = true },
                            ContextMenuItem("Delete") { showDeleteConfirmationDialog = true },
                        )
                    },
                ) {

                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        overlineContent = { Text(note.dateText) },
                        headlineContent = { Markdown(note.noteText) },
                    )

                    if (showEditDialog) {

                        val noteText = rememberTextFieldState(note.noteText)

                        NoteEditorWindow(
                            onCloseRequest = { showEditDialog = false },
                            noteId = note.id,
                            noteText = noteText,
                            onSaveNote = { onUpdateNote(note.id, noteText.text.toString()) },
                        )
                    }

                    if (showDeleteConfirmationDialog) {

                        DeleteConfirmationDialog(
                            subject = "note",
                            onDismiss = { showDeleteConfirmationDialog = false },
                            onConfirm = { onDeleteNote(note.id) },
                        )
                    }
                }

                HorizontalDivider()
            }
        }

        addWindowManager.Window {

            val noteText = rememberTextFieldState()

            NoteEditorWindow(
                onCloseRequest = addWindowManager::closeWindow,
                noteText = noteText,
                onSaveNote = { onAddNote(noteText.text.toString()) },
            )
        }
    }
}

@Composable
private fun NoteEditorWindow(
    onCloseRequest: () -> Unit,
    noteText: TextFieldState,
    onSaveNote: () -> Unit,
    noteId: TradeNoteId? = null,
) {

    val windowState = rememberAppWindowState(
        preferredPlacement = WindowPlacement.Floating,
        forcePreferredPlacement = true,
    )

    AppWindow(
        onCloseRequest = onCloseRequest,
        state = windowState,
        title = if (noteId != null) "Edit note ($noteId)" else "Add note",
    ) {

        Column(Modifier.fillMaxSize()) {

            var previewMarkdown by state { false }
            val initialFocusRequester = remember { FocusRequester() }

            LaunchedEffect(Unit) { initialFocusRequester.requestFocus() }

            Box(Modifier.weight(1F).fillMaxWidth()) {

                Crossfade(previewMarkdown) { previewMarkdown ->

                    when {
                        previewMarkdown -> {

                            val scrollState = rememberScrollState()

                            Markdown(
                                modifier = Modifier.fillMaxSize()
                                    .verticalScroll(scrollState)
                                    .padding(MaterialTheme.dimens.containerPadding),
                                content = noteText.toString(),
                            )

                            VerticalScrollbar(
                                modifier = Modifier.fillMaxSize().wrapContentWidth(Alignment.End),
                                adapter = rememberScrollbarAdapter(scrollState),
                            )
                        }

                        else -> TextField(
                            modifier = Modifier.fillMaxSize().focusRequester(initialFocusRequester),
                            state = noteText,
                        )
                    }
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.padding(MaterialTheme.dimens.containerPadding).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Text("Preview Markdown")

                Switch(
                    checked = previewMarkdown,
                    onCheckedChange = { previewMarkdown = it },
                )
            }

            HorizontalDivider()

            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onCloseRequest()
                    onSaveNote()
                },
                content = { Text("Save") },
            )
        }
    }
}
