package com.saurabhsandav.core.ui.reviews.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.paging.LoadState
import androidx.paging.PagingData
import com.saurabhsandav.core.thirdparty.paging.compose.LazyPagingItems
import com.saurabhsandav.core.thirdparty.paging.compose.collectAsLazyPagingItems
import com.saurabhsandav.core.thirdparty.paging.compose.itemContentType
import com.saurabhsandav.core.thirdparty.paging.compose.itemKey
import com.saurabhsandav.core.trades.model.ReviewId
import com.saurabhsandav.core.ui.common.DeleteConfirmationDialog
import com.saurabhsandav.core.ui.common.ListLoadStateIndicator
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.reviews.model.ReviewsState.ReviewEntry
import com.saurabhsandav.core.ui.reviews.model.ReviewsState.ReviewEntry.Item
import com.saurabhsandav.core.ui.reviews.model.ReviewsState.ReviewEntry.Section
import com.saurabhsandav.core.ui.theme.dimens
import kotlinx.coroutines.flow.Flow

@Composable
internal fun ReviewsList(
    reviewEntries: Flow<PagingData<ReviewEntry>>,
    onOpenReview: (ReviewId) -> Unit,
    onTogglePinReview: (ReviewId) -> Unit,
    onDeleteReview: (ReviewId) -> Unit,
) {

    val items = reviewEntries.collectAsLazyPagingItems()

    ListLoadStateIndicator(
        state = {
            when {
                items.loadState.refresh is LoadState.Loading -> loading()
                items.itemCount == 0 -> empty()
                else -> loaded()
            }
        },
        emptyText = { "No Reviews" },
    ) {

        ReviewsList(
            items = items,
            onOpenReview = onOpenReview,
            onTogglePinReview = onTogglePinReview,
            onDeleteReview = onDeleteReview,
        )
    }
}

@Composable
private fun ReviewsList(
    items: LazyPagingItems<ReviewEntry>,
    onOpenReview: (ReviewId) -> Unit,
    onTogglePinReview: (ReviewId) -> Unit,
    onDeleteReview: (ReviewId) -> Unit,
) {

    LazyColumn {

        items(
            count = items.itemCount,
            key = items.itemKey { entry ->

                when (entry) {
                    is Section -> "Section_${entry.isPinned}"
                    is Item -> entry.id
                }
            },
            contentType = items.itemContentType { entry -> entry.javaClass },
        ) { index ->

            when (val entry = items[index]!!) {
                is Section -> Section(
                    modifier = Modifier.animateItem(),
                    section = entry,
                )

                is Item -> Item(
                    modifier = Modifier.animateItem(),
                    item = entry,
                    onOpen = { onOpenReview(entry.id) },
                    isPinned = entry.isPinned,
                    onTogglePin = { onTogglePinReview(entry.id) },
                    onDelete = { onDeleteReview(entry.id) },
                )
            }
        }
    }
}

@Composable
private fun Section(
    modifier: Modifier,
    section: Section,
) {

    ListItem(
        modifier = Modifier.padding(MaterialTheme.dimens.listItemPadding).then(modifier),
        headlineContent = {
            Text(
                text = if (section.isPinned) "Pinned" else "Unpinned",
                style = MaterialTheme.typography.headlineLarge,
            )
        },
        supportingContent = {

            val count by section.count.collectAsState("")

            Text(
                text = "$count Reviews",
                style = MaterialTheme.typography.labelLarge,
            )
        },
    )

    HorizontalDivider()
}

@Composable
private fun Item(
    modifier: Modifier,
    item: Item,
    onOpen: () -> Unit,
    isPinned: Boolean,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
) {

    var showDeleteConfirmationDialog by state { false }

    // Passing the animateItem() modifier to ListItem doesn't work.
    // Use Box to workaround as the ContextMenuArea doesn't have a modifier parameter.
    Column(modifier) {

        ContextMenuArea(items = {
            listOf(
                ContextMenuItem("Open", onOpen),
                ContextMenuItem(if (!isPinned) "Pin" else "Unpin", onTogglePin),
                ContextMenuItem("Delete") { showDeleteConfirmationDialog = true },
            )
        }) {

            ListItem(
                modifier = Modifier.clickable(onClick = onOpen),
                headlineContent = { Text(item.title) },
            )
        }

        HorizontalDivider()
    }

    if (showDeleteConfirmationDialog) {

        DeleteConfirmationDialog(
            subject = "review",
            onDismiss = { showDeleteConfirmationDialog = false },
            onConfirm = onDelete,
        )
    }
}
