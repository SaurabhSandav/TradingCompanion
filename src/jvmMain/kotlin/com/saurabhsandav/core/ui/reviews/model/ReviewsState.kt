package com.saurabhsandav.core.ui.reviews.model

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.trades.model.ReviewId

@Immutable
internal data class ReviewsState(
    val pinnedReviews: List<Review>,
    val unPinnedReviews: List<Review>,
    val eventSink: (ReviewsEvent) -> Unit,
) {

    @Immutable
    internal data class Review(
        val id: ReviewId,
        val title: String,
    )
}
