package com.saurabhsandav.core.ui.review.ui

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.saurabhsandav.core.ui.common.state

@Composable
internal fun ReviewTopAppBar(
    title: String,
    onSetTitle: (String) -> Unit,
    isMarkdown: Boolean,
    onToggleMarkdown: () -> Unit,
    isReviewEditMode: Boolean,
    onToggleEditReview: () -> Unit,
    canSaveReview: Boolean,
    onSaveReview: () -> Unit,
) {

    TopAppBar(
        title = {

            var edit by state { false }

            when {
                edit -> {

                    var editingTitle by state { title }
                    val focusRequester = remember { FocusRequester() }
                    LaunchedEffect(Unit) { focusRequester.requestFocus() }

                    TextField(
                        modifier = Modifier
                            .onKeyEvent { keyEvent ->

                                when (keyEvent.key) {
                                    Key.Enter, Key.NumPadEnter -> {
                                        if (editingTitle.isNotEmpty())
                                            onSetTitle(editingTitle)
                                    }

                                    Key.Escape -> editingTitle = title
                                    else -> return@onKeyEvent false
                                }

                                edit = false

                                return@onKeyEvent true
                            }
                            .focusRequester(focusRequester),
                        value = editingTitle,
                        onValueChange = { editingTitle = it },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background,
                        )
                    )
                }

                else -> Text(
                    modifier = Modifier.clickable { edit = true },
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        actions = {

            IconButton(
                onClick = onToggleMarkdown,
            ) {

                when {
                    isMarkdown -> Icon(Icons.Default.Title, contentDescription = "Disable Markdown")
                    else -> Icon(painterResource("icons/markdown.svg"), contentDescription = "Enable Markdown")
                }
            }

            IconButton(
                onClick = onToggleEditReview,
            ) {

                Icon(
                    imageVector = when {
                        isReviewEditMode -> Icons.Default.Preview
                        else -> Icons.Default.Edit
                    },
                    contentDescription = "Edit Review",
                )
            }

            IconButton(
                onClick = onSaveReview,
                enabled = canSaveReview,
            ) {
                Icon(Icons.Default.Save, contentDescription = "Save Review")
            }
        },
    )
}
