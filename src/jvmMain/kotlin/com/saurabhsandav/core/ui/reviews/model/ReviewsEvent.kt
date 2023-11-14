package com.saurabhsandav.core.ui.reviews.model

import com.saurabhsandav.core.ui.tradecontent.ProfileReviewId

internal sealed class ReviewsEvent {

    data object NewReview : ReviewsEvent()

    data class OpenReview(val profileReviewId: ProfileReviewId) : ReviewsEvent()

    data class DeleteReview(val profileReviewId: ProfileReviewId) : ReviewsEvent()
}
