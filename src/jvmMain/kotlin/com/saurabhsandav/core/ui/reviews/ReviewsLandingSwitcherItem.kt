package com.saurabhsandav.core.ui.reviews

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.saurabhsandav.core.ui.landing.LandingSwitcherItem
import com.saurabhsandav.core.ui.reviews.model.ReviewsEvent.*

internal class ReviewsLandingSwitcherItem(
    reviewsModule: ReviewsModule,
) : LandingSwitcherItem {

    private val presenter = reviewsModule.presenter()

    @Composable
    override fun Content() {

        val state by presenter.state.collectAsState()

        ReviewsScreen(
            reviews = state.reviews,
            onNewReview = { state.eventSink(NewReview) },
            onOpenReview = { id -> state.eventSink(OpenReview(id)) },
            onDeleteReview = { id -> state.eventSink(DeleteReview(id)) },
        )
    }
}
