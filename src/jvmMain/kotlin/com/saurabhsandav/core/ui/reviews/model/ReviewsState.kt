package com.saurabhsandav.core.ui.reviews.model

import com.saurabhsandav.core.trades.model.ReviewId

internal data class ReviewsState(
    val pinnedReviews: List<Review>,
    val unPinnedReviews: List<Review>,
    val eventSink: (ReviewsEvent) -> Unit,
) {

    internal data class Review(
        val id: ReviewId,
        val title: String,
    )
}
