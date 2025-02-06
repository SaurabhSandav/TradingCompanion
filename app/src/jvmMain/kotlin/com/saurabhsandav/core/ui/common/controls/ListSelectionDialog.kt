package com.saurabhsandav.core.ui.common.controls

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.DpSize
import com.saurabhsandav.core.ui.common.app.AppDialog
import com.saurabhsandav.core.ui.common.derivedState
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.theme.dimens
import com.saurabhsandav.core.ui.theme.keyboardSelectionBackgroundColor
import kotlinx.coroutines.flow.collectLatest

@Composable
fun <T : Any> ListSelectionDialog(
    onDismissRequest: () -> Unit,
    items: List<T>,
    itemText: (T) -> String,
    onSelect: (T) -> Unit,
    title: @Composable (() -> Unit)? = null,
    initialFilterQuery: String = "",
    dialogSize: DpSize = MaterialTheme.dimens.dialogSize,
    itemTrailingContent: @Composable ((T) -> Unit)? = null,
) {

    AppDialog(
        onDismissRequest = onDismissRequest,
        size = dialogSize,
    ) {

        Column {

            var selectedIndex by state { -1 }
            val scrollState = rememberScrollState()
            val focusRequester = remember { FocusRequester() }

            var filterQuery by state {
                TextFieldValue(initialFilterQuery, TextRange(initialFilterQuery.length))
            }
            val filteredItems by derivedState {
                items.filter { item -> itemText(item).contains(filterQuery.text, ignoreCase = true) }
            }

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent { keyEvent ->

                        if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                        when (keyEvent.key) {
                            Key.DirectionUp -> selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                            Key.DirectionDown -> selectedIndex =
                                (selectedIndex + 1).coerceAtMost(filteredItems.lastIndex)

                            Key.Enter -> {
                                onSelect(filteredItems[selectedIndex])
                                onDismissRequest()
                            }

                            else -> return@onPreviewKeyEvent false
                        }

                        true
                    },
                value = filterQuery,
                onValueChange = {
                    selectedIndex = -1
                    filterQuery = it
                },
                singleLine = true,
                placeholder = title,
            )

            Column(
                modifier = Modifier.selectableGroup().verticalScroll(scrollState),
            ) {

                filteredItems.forEachIndexed { index, item ->

                    key(item) {

                        val isSelected = selectedIndex == index
                        val bringIntoViewRequester = remember { BringIntoViewRequester() }

                        // Scroll item into view if selected
                        LaunchedEffect(isSelected) {
                            if (isSelected) bringIntoViewRequester.bringIntoView()
                        }

                        ListItem(
                            modifier = Modifier
                                .selectable(selected = selectedIndex == index) {
                                    onSelect(item)
                                    onDismissRequest()
                                }
                                .bringIntoViewRequester(bringIntoViewRequester),
                            headlineContent = { Text(itemText(item)) },
                            trailingContent = itemTrailingContent?.let { { it(item) } },
                            colors = ListItemDefaults.colors(
                                containerColor = when {
                                    selectedIndex == index -> keyboardSelectionBackgroundColor()
                                    else -> Color.Unspecified
                                }
                            ),
                        )
                    }

                    if (index != filteredItems.lastIndex) HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun <T : Any> LazyListSelectionDialog(
    onDismissRequest: () -> Unit,
    items: List<T>,
    itemText: (T) -> String,
    onSelect: (T) -> Unit,
    onFilter: (String) -> Unit,
    title: @Composable (() -> Unit)? = null,
    initialFilterQuery: String = "",
    dialogSize: DpSize = MaterialTheme.dimens.dialogSize,
    itemTrailingContent: @Composable ((T) -> Unit)? = null,
) {

    AppDialog(
        onDismissRequest = onDismissRequest,
        size = dialogSize,
    ) {

        Column {

            var selectedIndex by state { -1 }
            val lazyListState = rememberLazyListState()
            val focusRequester = remember { FocusRequester() }

            var filterQuery by state {
                TextFieldValue(initialFilterQuery, TextRange(initialFilterQuery.length))
            }

            // Scroll selected item to center of list
            LaunchedEffect(Unit) {

                snapshotFlow { selectedIndex }.collectLatest { index ->
                    if (index == -1) return@collectLatest
                    lazyListState.scrollItemToCenterOfList(index)
                }
            }

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent { keyEvent ->

                        if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                        when (keyEvent.key) {
                            Key.DirectionUp -> selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                            Key.DirectionDown -> selectedIndex =
                                (selectedIndex + 1).coerceAtMost(items.lastIndex)

                            Key.Enter -> {
                                onSelect(items[selectedIndex])
                                onDismissRequest()
                            }

                            else -> return@onPreviewKeyEvent false
                        }

                        true
                    },
                value = filterQuery,
                onValueChange = {
                    selectedIndex = -1
                    filterQuery = it
                    onFilter(it.text)
                },
                singleLine = true,
                placeholder = title,
            )

            Box(Modifier.weight(1F)) {

                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.selectableGroup(),
                ) {

                    itemsIndexed(
                        items = items,
                        key = { _, item -> item },
                    ) { index, item ->

                        ListItem(
                            modifier = Modifier
                                .animateItem()
                                .selectable(selected = selectedIndex == index) {
                                    onSelect(item)
                                    onDismissRequest()
                                },
                            headlineContent = { Text(itemText(item)) },
                            trailingContent = itemTrailingContent?.let { { it(item) } },
                            colors = ListItemDefaults.colors(
                                containerColor = when {
                                    selectedIndex == index -> keyboardSelectionBackgroundColor()
                                    else -> Color.Unspecified
                                }
                            ),
                        )

                        if (index != items.lastIndex) HorizontalDivider()
                    }
                }

                VerticalScrollbar(
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                    adapter = rememberScrollbarAdapter(lazyListState),
                )
            }
        }
    }
}

suspend fun LazyListState.scrollItemToCenterOfList(itemIndex: Int) {

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
