package com.saurabhsandav.core.ui.reviews.model

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.ui.tradecontent.ProfileReviewId
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class ReviewsState(
    val reviews: ImmutableList<Review>,
    val eventSink: (ReviewsEvent) -> Unit,
) {

    @Immutable
    internal data class Review(
        val profileReviewId: ProfileReviewId,
        val title: String,
    )
}
