package com.saurabhsandav.core.ui.reviews.model

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.trades.model.ReviewId
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class ReviewsState(
    val reviews: ImmutableList<Review>,
    val eventSink: (ReviewsEvent) -> Unit,
) {

    @Immutable
    internal data class Review(
        val id: ReviewId,
        val title: String,
    )
}
