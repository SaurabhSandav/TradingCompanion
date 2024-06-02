package com.saurabhsandav.core.ui.reviews

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.trades.TradesRepo
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.model.ReviewId
import com.saurabhsandav.core.ui.reviews.model.ReviewsEvent
import com.saurabhsandav.core.ui.reviews.model.ReviewsEvent.*
import com.saurabhsandav.core.ui.reviews.model.ReviewsState
import com.saurabhsandav.core.ui.reviews.model.ReviewsState.Review
import com.saurabhsandav.core.ui.tradecontent.ProfileReviewId
import com.saurabhsandav.core.ui.tradecontent.TradeContentLauncher
import com.saurabhsandav.core.utils.emitInto
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

internal class ReviewsPresenter(
    private val coroutineScope: CoroutineScope,
    private val profileId: ProfileId,
    private val tradeContentLauncher: TradeContentLauncher,
    private val tradingProfiles: TradingProfiles,
) {

    private val tradesRepo = coroutineScope.async { tradingProfiles.getRecord(profileId).trades }

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule ReviewsState(
            pinnedReviews = getPinnedReviews(),
            unPinnedReviews = getUnpinnedReviews(),
            eventSink = ::onEvent,
        )
    }

    private fun onEvent(event: ReviewsEvent) {

        when (event) {
            NewReview -> onNewReview()
            is OpenReview -> onOpenReview(event.id)
            is TogglePinReview -> onTogglePinReview(event.id)
            is DeleteReview -> onDeleteReview(event.id)
        }
    }

    @Composable
    private fun getPinnedReviews(): List<Review> = getReviews { getPinnedReviews() }

    @Composable
    private fun getUnpinnedReviews(): List<Review> = getReviews { getUnPinnedReviews() }

    @Composable
    private fun getReviews(
        query: TradesRepo.() -> Flow<List<com.saurabhsandav.core.trades.Review>>,
    ): List<Review> {
        return remember {
            flow {

                tradesRepo
                    .await()
                    .query()
                    .map { reviews ->
                        reviews
                            .map { review ->

                                Review(
                                    id = review.id,
                                    title = review.title,
                                )
                            }
                    }
                    .emitInto(this)
            }
        }.collectAsState(emptyList()).value
    }

    private fun onNewReview() = coroutineScope.launchUnit {

        val reviewId = tradesRepo.await().createReview(
            title = "New Review",
            tradeIds = emptyList(),
            review = "",
            isMarkdown = false,
        )

        val profileReviewId = ProfileReviewId(
            profileId = profileId,
            reviewId = reviewId,
        )

        tradeContentLauncher.openReview(profileReviewId)
    }

    private fun onOpenReview(id: ReviewId) {

        tradeContentLauncher.openReview(ProfileReviewId(profileId = profileId, reviewId = id))
    }

    private fun onTogglePinReview(id: ReviewId) = coroutineScope.launchUnit {

        tradesRepo.await().togglePinReview(id)
    }

    private fun onDeleteReview(id: ReviewId) = coroutineScope.launchUnit {

        tradesRepo.await().deleteReview(id)
    }
}
