package com.saurabhsandav.core.ui.reviews.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.reviews.model.ReviewsState.Review
import com.saurabhsandav.core.ui.tradecontent.ProfileReviewId
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun ReviewsList(
    reviews: ImmutableList<Review>,
    onOpenReview: (ProfileReviewId) -> Unit,
    onDeleteReview: (ProfileReviewId) -> Unit,
) {

    LazyColumn {

        items(
            items = reviews,
            key = { it.profileReviewId },
        ) { review ->

            ReviewItem(
                review = review,
                onOpenReview = { onOpenReview(review.profileReviewId) },
                onDeleteReview = { onDeleteReview(review.profileReviewId) },
            )
        }
    }
}

@Composable
private fun ReviewItem(
    review: Review,
    onOpenReview: () -> Unit,
    onDeleteReview: () -> Unit,
) {

    var showDeleteConfirmationDialog by state { false }

    ContextMenuArea(items = {
        listOf(
            ContextMenuItem("Open", onOpenReview),
            ContextMenuItem("Delete") { showDeleteConfirmationDialog = true }
        )
    }) {

        ListItem(
            modifier = Modifier.clickable(onClick = onOpenReview),
            headlineContent = { Text(review.title) },
        )

        Divider()
    }

    if (showDeleteConfirmationDialog) {

        DeleteConfirmationDialog(
            onDismiss = { showDeleteConfirmationDialog = false },
            onConfirm = onDeleteReview,
        )
    }
}

@Composable
private fun DeleteConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Yes")
            }
        },
        text = {
            Text("Are you sure you want to delete the review?")
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("No")
            }
        },
    )
}
