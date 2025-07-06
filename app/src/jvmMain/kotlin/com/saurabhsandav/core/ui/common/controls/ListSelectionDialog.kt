package com.saurabhsandav.core.ui.common.controls

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.DpSize
import com.saurabhsandav.core.ui.common.BoxWithScrollbar
import com.saurabhsandav.core.ui.common.app.AppDialog
import com.saurabhsandav.core.ui.theme.dimens
import com.saurabhsandav.core.ui.theme.keyboardSelectionBackgroundColor

@Composable
fun ListSelectionDialog(
    onDismissRequest: () -> Unit,
    itemCount: () -> Int,
    selectedIndex: Int,
    onSelectionChange: (Int) -> Unit,
    onSelectionFinished: (Int) -> Unit,
    key: ((index: Int) -> Any)? = null,
    filterQuery: TextFieldValue = TextFieldValue(),
    onFilterChange: (TextFieldValue) -> Unit = {},
    title: @Composable (() -> Unit)? = null,
    isLoading: Boolean = false,
    onKeyEvent: ((KeyEvent, Int) -> Boolean)? = null,
    dialogSize: DpSize = MaterialTheme.dimens.dialogSize,
    itemContent: @Composable ListSelectionDialogItemScope.(Int) -> Unit,
) {

    AppDialog(
        onDismissRequest = onDismissRequest,
    ) {

        Column(
            modifier = Modifier
                .width(dialogSize.width)
                .heightIn(max = dialogSize.height),
        ) {

            val lazyListState = rememberLazyListState()
            val focusRequester = remember { FocusRequester() }

            // Scroll selected item to center of list
            LaunchedEffect(selectedIndex) {
                if (selectedIndex == -1) return@LaunchedEffect
                lazyListState.scrollItemToCenterOfList(selectedIndex)
            }

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent { keyEvent ->

                        if (keyEvent.type == KeyEventType.KeyDown) {

                            var consumed = true

                            when (keyEvent.key) {
                                Key.DirectionUp -> onSelectionChange((selectedIndex - 1).coerceAtLeast(0))
                                Key.DirectionDown ->
                                    onSelectionChange((selectedIndex + 1).coerceAtMost(itemCount() - 1))

                                Key.Enter if (selectedIndex != -1) -> {
                                    onSelectionFinished(selectedIndex)
                                    onDismissRequest()
                                }

                                else -> consumed = false
                            }

                            if (consumed) return@onPreviewKeyEvent true
                        }

                        if (onKeyEvent != null && selectedIndex != -1) {
                            return@onPreviewKeyEvent onKeyEvent(keyEvent, selectedIndex)
                        }

                        false
                    },
                value = filterQuery,
                onValueChange = onFilterChange,
                singleLine = true,
                placeholder = title,
            )

            BoxWithScrollbar(
                modifier = Modifier.weight(1F, fill = false).animateContentSize(),
                scrollbarAdapter = rememberScrollbarAdapter(lazyListState),
            ) {

                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.selectableGroup(),
                ) {

                    items(
                        count = itemCount(),
                        key = key,
                    ) { index ->

                        val scope = remember(this) { ListSelectionDialogItemScopeImpl(this) }

                        scope.itemContent(index)

                        if (index != itemCount() - 1) HorizontalDivider()
                    }

                    if (isLoading) {

                        item {

                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(MaterialTheme.dimens.containerPadding)
                                    .fillMaxWidth()
                                    .wrapContentWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

interface ListSelectionDialogItemScope : LazyItemScope {

    @Composable
    fun ListSelectionItem(
        isSelected: Boolean,
        onSelect: () -> Unit,
        headlineContent: @Composable () -> Unit,
        overlineContent: @Composable (() -> Unit)? = null,
        supportingContent: @Composable (() -> Unit)? = null,
        leadingContent: @Composable (() -> Unit)? = null,
        trailingContent: @Composable (() -> Unit)? = null,
    )
}

private class ListSelectionDialogItemScopeImpl(
    lazyItemScope: LazyItemScope,
) : ListSelectionDialogItemScope,
    LazyItemScope by lazyItemScope {

    @Composable
    override fun ListSelectionItem(
        isSelected: Boolean,
        onSelect: () -> Unit,
        headlineContent: @Composable () -> Unit,
        overlineContent: @Composable (() -> Unit)?,
        supportingContent: @Composable (() -> Unit)?,
        leadingContent: @Composable (() -> Unit)?,
        trailingContent: @Composable (() -> Unit)?,
    ) {

        ListItem(
            modifier = Modifier
                .animateItem()
                .selectable(selected = isSelected, onClick = onSelect),
            headlineContent = headlineContent,
            trailingContent = trailingContent,
            overlineContent = overlineContent,
            supportingContent = supportingContent,
            leadingContent = leadingContent,
            colors = ListItemDefaults.colors(
                containerColor = if (isSelected) keyboardSelectionBackgroundColor() else Color.Unspecified,
            ),
        )
    }
}

private suspend fun LazyListState.scrollItemToCenterOfList(itemIndex: Int) {

    val itemInfo = layoutInfo
        .visibleItemsInfo
        .find { it.index == itemIndex }

    if (itemInfo != null) {
        val listCenter = layoutInfo.viewportEndOffset / 2
        val itemCenter = itemInfo.offset + (itemInfo.size / 2)
        animateScrollBy((itemCenter - listCenter).toFloat())
    } else {
        animateScrollToItem(itemIndex)
    }
}
