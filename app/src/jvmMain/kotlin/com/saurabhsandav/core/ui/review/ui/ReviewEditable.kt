package com.saurabhsandav.core.ui.review.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import com.saurabhsandav.core.ui.common.BoxWithScrollbar
import com.saurabhsandav.core.ui.theme.dimens
import kotlinx.coroutines.flow.collectLatest

@Composable
internal fun ReviewEditable(
    edit: Boolean,
    review: String,
    onReviewChange: (String) -> Unit,
    onSaveReview: () -> Unit,
    onMarkdownLinkClicked: (String) -> Unit,
) {

    val scrollState = rememberScrollState()

    BoxWithScrollbar(
        scrollbarAdapter = rememberScrollbarAdapter(scrollState),
    ) {

        val textFieldState = rememberTextFieldState(review)

        LaunchedEffect(textFieldState) {
            snapshotFlow { textFieldState.text.toString() }.collectLatest { review -> onReviewChange(review) }
        }

        Crossfade(edit) { editTarget ->

            val modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(MaterialTheme.dimens.containerPadding)

            when {
                editTarget -> {

                    val initialFocusRequester = remember { FocusRequester() }
                    LaunchedEffect(Unit) { initialFocusRequester.requestFocus() }

                    TextField(
                        modifier = modifier
                            .focusRequester(initialFocusRequester)
                            .onPreviewKeyEvent { keyEvent ->

                                return@onPreviewKeyEvent when {
                                    keyEvent.isCtrlPressed &&
                                        keyEvent.type == KeyEventType.KeyDown &&
                                        keyEvent.key == Key.S -> {
                                        onSaveReview()
                                        true
                                    }

                                    else -> false
                                }
                            },
                        state = textFieldState,
                    )
                }

                else -> {

                    val onMarkdownLinkClickedUpdated by rememberUpdatedState(onMarkdownLinkClicked)

                    val uriHandler = remember {
                        object : UriHandler {
                            override fun openUri(uri: String) {
                                onMarkdownLinkClickedUpdated(uri)
                            }
                        }
                    }

                    CompositionLocalProvider(LocalUriHandler provides uriHandler) {

                        Markdown(
                            modifier = modifier,
                            content = review,
                            typography = markdownTypography(
                                textLink = TextLinkStyles(
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        textDecoration = TextDecoration.Underline,
                                        color = MaterialTheme.colorScheme.inversePrimary,
                                    ).toSpanStyle(),
                                ),
                            ),
                        )
                    }
                }
            }
        }
    }
}
