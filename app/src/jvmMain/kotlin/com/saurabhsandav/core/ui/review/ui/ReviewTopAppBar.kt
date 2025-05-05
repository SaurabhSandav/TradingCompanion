package com.saurabhsandav.core.ui.review.ui

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.style.TextOverflow
import com.saurabhsandav.core.ui.common.IconButtonWithTooltip
import com.saurabhsandav.core.ui.common.state

@Composable
internal fun ReviewTopAppBar(
    title: String,
    onSetTitle: (String) -> Unit,
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
                                        if (editingTitle.isNotEmpty()) {
                                            onSetTitle(editingTitle)
                                        }
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
                        ),
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

            IconButtonWithTooltip(
                onClick = onToggleEditReview,
                tooltipText = "Edit Review",
            ) {

                Icon(
                    imageVector = when {
                        isReviewEditMode -> Icons.Default.Preview
                        else -> Icons.Default.Edit
                    },
                    contentDescription = "Edit Review",
                )
            }

            IconButtonWithTooltip(
                onClick = onSaveReview,
                tooltipText = "Save Review",
                enabled = canSaveReview,
            ) {
                Icon(Icons.Default.Save, contentDescription = "Save Review")
            }
        },
    )
}
