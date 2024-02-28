package com.saurabhsandav.core.ui.reviews.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.trades.model.ReviewId
import com.saurabhsandav.core.ui.common.DeleteConfirmationDialog
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.reviews.model.ReviewsState.Review
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun ReviewsList(
    pinnedReviews: ImmutableList<Review>,
    unPinnedReviews: ImmutableList<Review>,
    onOpenReview: (ReviewId) -> Unit,
    onTogglePinReview: (ReviewId) -> Unit,
    onDeleteReview: (ReviewId) -> Unit,
) {

    LazyColumn {

        reviews(
            isPinned = true,
            reviews = pinnedReviews,
            onOpenReview = onOpenReview,
            onTogglePinReview = onTogglePinReview,
            onDeleteReview = onDeleteReview,
        )

        reviews(
            isPinned = false,
            reviews = unPinnedReviews,
            onOpenReview = onOpenReview,
            onTogglePinReview = onTogglePinReview,
            onDeleteReview = onDeleteReview,
        )
    }
}

private fun LazyListScope.reviews(
    isPinned: Boolean,
    reviews: ImmutableList<Review>,
    onOpenReview: (ReviewId) -> Unit,
    onTogglePinReview: (ReviewId) -> Unit,
    onDeleteReview: (ReviewId) -> Unit,
) {

    if (reviews.isNotEmpty()) {

        item(
            contentType = ContentType.Header,
        ) {

            ListItem(
                modifier = Modifier.padding(8.dp),
                headlineContent = {
                    Text(
                        text = if (isPinned) "Pinned" else "Unpinned",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                supportingContent = {
                    Text(
                        text = "${reviews.size} Reviews",
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
            )
        }

        items(
            items = reviews,
            key = { it.id },
            contentType = { ContentType.Review },
        ) { review ->

            ReviewItem(
                review = review,
                onOpen = { onOpenReview(review.id) },
                isPinned = isPinned,
                onTogglePin = { onTogglePinReview(review.id) },
                onDelete = { onDeleteReview(review.id) },
            )
        }
    }
}

@Composable
private fun ReviewItem(
    review: Review,
    onOpen: () -> Unit,
    isPinned: Boolean,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
) {

    var showDeleteConfirmationDialog by state { false }

    ContextMenuArea(items = {
        listOf(
            ContextMenuItem("Open", onOpen),
            ContextMenuItem(if (!isPinned) "Pin" else "Unpin", onTogglePin),
            ContextMenuItem("Delete") { showDeleteConfirmationDialog = true }
        )
    }) {

        ListItem(
            modifier = Modifier.clickable(onClick = onOpen),
            headlineContent = { Text(review.title) },
        )

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

private enum class ContentType {
    Header, Review;
}
