package com.saurabhsandav.core.ui.reviews.model

import com.saurabhsandav.core.trades.model.ReviewId

internal sealed class ReviewsEvent {

    data object NewReview : ReviewsEvent()

    data class OpenReview(
        val id: ReviewId,
    ) : ReviewsEvent()

    data class TogglePinReview(
        val id: ReviewId,
    ) : ReviewsEvent()

    data class DeleteReview(
        val id: ReviewId,
    ) : ReviewsEvent()
}
