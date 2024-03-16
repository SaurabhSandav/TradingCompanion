package com.saurabhsandav.core.ui.review.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.RichTextStyle
import com.halilibo.richtext.ui.material3.RichText
import com.halilibo.richtext.ui.string.RichTextStringStyle
import com.saurabhsandav.core.ui.common.saveableState

@Composable
internal fun ReviewEditable(
    isMarkdown: Boolean,
    edit: Boolean,
    review: String,
    onReviewChange: (String) -> Unit,
    onSaveReview: () -> Unit,
    onMarkdownLinkClicked: (String) -> Unit,
) {

    Box {

        val scrollState = rememberScrollState()
        var textFieldValue by saveableState(stateSaver = TextFieldValue.Saver) { TextFieldValue(review) }

        Crossfade(edit) { editTarget ->

            val modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)

            when {
                editTarget -> {

                    val initialFocusRequester = remember { FocusRequester() }
                    LaunchedEffect(Unit) { initialFocusRequester.requestFocus() }

                    TextField(
                        modifier = modifier
                            .focusRequester(initialFocusRequester)
                            .onPreviewKeyEvent { keyEvent ->

                                return@onPreviewKeyEvent when {
                                    keyEvent.isCtrlPressed
                                            && keyEvent.type == KeyEventType.KeyDown
                                            && keyEvent.key == Key.S
                                    -> {
                                        onSaveReview()
                                        true
                                    }

                                    else -> false
                                }
                            },
                        value = textFieldValue.copy(text = review),
                        onValueChange = {
                            textFieldValue = it
                            onReviewChange(it.text)
                        },
                    )
                }

                isMarkdown -> RichText(
                    modifier = modifier,
                    // TODO Temporary style. Built-in styling for link is terrible in dark mode.
                    //  Replace if library updated with better defaults
                    style = RichTextStyle(
                        stringStyle = RichTextStringStyle(
                            linkStyle = SpanStyle(color = MaterialTheme.colorScheme.inversePrimary),
                        ),
                    ),
                    linkClickHandler = onMarkdownLinkClicked,
                    children = { Markdown(content = review) },
                )

                else -> Text(
                    modifier = modifier,
                    text = review,
                )
            }
        }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(scrollState),
        )
    }
}
