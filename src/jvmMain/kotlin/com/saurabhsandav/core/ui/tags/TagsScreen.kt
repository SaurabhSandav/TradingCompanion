package com.saurabhsandav.core.ui.tags

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.trades.model.TradeTagId
import com.saurabhsandav.core.ui.tags.model.TagsState.Tag
import com.saurabhsandav.core.ui.tags.ui.TagListItem

@Composable
fun TagsScreen(
    tags: List<Tag>,
    onNewTag: () -> Unit,
    onNewTagFromExisting: (TradeTagId) -> Unit,
    onEditTag: (TradeTagId) -> Unit,
    onDeleteTag: (TradeTagId) -> Unit,
) {

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onNewTag) {
                Text(text = "New Tag")
            }
        },
    ) {

        Box {

            val lazyListState = rememberLazyListState()

            LazyColumn(
                state = lazyListState,
            ) {

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
}
