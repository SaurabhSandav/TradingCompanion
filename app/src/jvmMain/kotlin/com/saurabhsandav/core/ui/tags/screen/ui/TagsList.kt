package com.saurabhsandav.core.ui.tags.screen.ui

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.trades.model.TradeTagId
import com.saurabhsandav.core.ui.common.ListLoadStateIndicator
import com.saurabhsandav.core.ui.tags.model.TradeTag
import com.saurabhsandav.core.ui.theme.dimens

@Composable
fun TagsList(
    tags: List<TradeTag>?,
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
            onNewTagFromExisting = onNewTagFromExisting,
            onEditTag = onEditTag,
            onDeleteTag = onDeleteTag,
        )
    }
}

@JvmName("TagsListActual")
@Composable
private fun TagsList(
    tags: List<TradeTag>,
    onNewTagFromExisting: (TradeTagId) -> Unit,
    onEditTag: (TradeTagId) -> Unit,
    onDeleteTag: (TradeTagId) -> Unit,
) {

    Box {

        val lazyGridState = rememberLazyGridState()

        LazyVerticalGrid(
            state = lazyGridState,
            columns = GridCells.Adaptive(400.dp),
            contentPadding = PaddingValues(MaterialTheme.dimens.containerPadding),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.rowHorizontalSpacing),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.columnVerticalSpacing),
        ) {

            items(
                items = tags,
                key = { it.id },
            ) { tag ->

                TagCard(
                    modifier = Modifier.animateItem(),
                    tag = tag,
                    onNewTag = { onNewTagFromExisting(tag.id) },
                    onEditTag = { onEditTag(tag.id) },
                    onDelete = { onDeleteTag(tag.id) },
                )
            }
        }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(lazyGridState)
        )
    }
}
