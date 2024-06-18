package com.saurabhsandav.core.ui.tags.ui

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.trades.model.TradeTagId
import com.saurabhsandav.core.ui.common.ListLoadStateIndicator
import com.saurabhsandav.core.ui.common.PrimaryOptionsBar
import com.saurabhsandav.core.ui.tags.model.TagsState.Tag

@Composable
fun TagsList(
    tags: List<Tag>?,
    onNewTag: () -> Unit,
    onNewTagFromExisting: (TradeTagId) -> Unit,
    onEditTag: (TradeTagId) -> Unit,
    onDeleteTag: (TradeTagId) -> Unit,
) {

    ListLoadStateIndicator(
        state = {
            when {
                tags == null -> loading()
                tags.isEmpty() -> empty()
                else -> loaded()
            }
        },
        emptyText = { "No Tags" },
    ) {

        TagsList(
            tags = tags ?: emptyList(),
            onNewTag = onNewTag,
            onNewTagFromExisting = onNewTagFromExisting,
            onEditTag = onEditTag,
            onDeleteTag = onDeleteTag,
        )
    }
}

@JvmName("TagsListActual")
@Composable
private fun TagsList(
    tags: List<Tag>,
    onNewTag: () -> Unit,
    onNewTagFromExisting: (TradeTagId) -> Unit,
    onEditTag: (TradeTagId) -> Unit,
    onDeleteTag: (TradeTagId) -> Unit,
) {

    Box {

        val lazyListState = rememberLazyListState()

        LazyColumn(
            state = lazyListState,
        ) {

            stickyHeader {

                Header(
                    onNewTag = onNewTag,
                )
            }

            items(
                items = tags,
                key = { it.id },
            ) { tag ->

                TagListItem(
                    tag = tag,
                    onNewTag = { onNewTagFromExisting(tag.id) },
                    onEditTag = { onEditTag(tag.id) },
                    onDelete = { onDeleteTag(tag.id) },
                )
            }
        }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(lazyListState)
        )
    }
}

@Composable
private fun Header(onNewTag: () -> Unit) {

    Surface {

        Column {

            PrimaryOptionsBar {

                Button(
                    onClick = onNewTag,
                    shape = MaterialTheme.shapes.small,
                    content = { Text("New Tag") },
                )
            }

            HorizontalDivider()
        }
    }
}
