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
import androidx.compose.ui.window.WindowPlacement
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.material3.RichText
import com.saurabhsandav.core.trades.model.TradeNoteId
import com.saurabhsandav.core.ui.common.DeleteConfirmationDialog
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.app.AppWindowManager
import com.saurabhsandav.core.ui.common.app.rememberAppWindowState
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.theme.dimens
import com.saurabhsandav.core.ui.trade.model.TradeState.TradeNote

@Composable
internal fun Notes(
    notes: List<TradeNote>,
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

        HorizontalDivider()

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

                        DeleteConfirmationDialog(
                            subject = "note",
                            onDismiss = { showDeleteConfirmationDialog = false },
                            onConfirm = { onDeleteNote(note.id) },
                        )
                    }
                }
            }

            HorizontalDivider()
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
                                modifier = Modifier.fillMaxSize()
                                    .verticalScroll(scrollState)
                                    .padding(MaterialTheme.dimens.containerPadding),
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

            HorizontalDivider()

            Column(
                modifier = Modifier.padding(MaterialTheme.dimens.containerPadding),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.columnVerticalSpacing),
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

            HorizontalDivider()

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
