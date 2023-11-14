package com.saurabhsandav.core.ui.reviews

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.ui.reviews.model.ReviewsEvent
import com.saurabhsandav.core.ui.reviews.model.ReviewsEvent.*
import com.saurabhsandav.core.ui.reviews.model.ReviewsState
import com.saurabhsandav.core.ui.reviews.model.ReviewsState.Review
import com.saurabhsandav.core.ui.tradecontent.ProfileReviewId
import com.saurabhsandav.core.ui.tradecontent.TradeContentLauncher
import com.saurabhsandav.core.utils.getCurrentTradingProfile
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@Stable
internal class ReviewsPresenter(
    private val coroutineScope: CoroutineScope,
    private val tradeContentLauncher: TradeContentLauncher,
    private val tradingProfiles: TradingProfiles,
    private val appPrefs: FlowSettings,
) {

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule ReviewsState(
            reviews = getReviews(),
            eventSink = ::onEvent,
        )
    }

    private fun onEvent(event: ReviewsEvent) {

        when (event) {
            NewReview -> onNewReview()
            is OpenReview -> onOpenReview(event.profileReviewId)
            is DeleteReview -> onDeleteReview(event.profileReviewId)
        }
    }

    @Composable
    private fun getReviews(): ImmutableList<Review> {
        return remember {

            appPrefs.getCurrentTradingProfile(tradingProfiles).flatMapLatest { profile ->

                val tradingRecord = tradingProfiles.getRecord(profile.id)

                tradingRecord.trades.allReviews.map { reviews ->
                    reviews
                        .map { review ->

                            Review(
                                profileReviewId = ProfileReviewId(
                                    profileId = profile.id,
                                    reviewId = review.id,
                                ),
                                title = review.title,
                            )
                        }
                        .toImmutableList()
                }
            }
        }.collectAsState(persistentListOf()).value
    }

    private fun onNewReview() = coroutineScope.launchUnit {

        val profile = appPrefs.getCurrentTradingProfile(tradingProfiles).first()
        val record = tradingProfiles.getRecord(profile.id)

        val reviewId = record.trades.createReview(
            title = "New Review",
            tradeIds = emptyList(),
            review = "",
            isMarkdown = false,
        )

        val profileReviewId = ProfileReviewId(
            profileId = profile.id,
            reviewId = reviewId,
        )

        tradeContentLauncher.openReview(profileReviewId)
    }

    private fun onOpenReview(profileReviewId: ProfileReviewId) {

        tradeContentLauncher.openReview(profileReviewId)
    }

    private fun onDeleteReview(profileReviewId: ProfileReviewId) = coroutineScope.launchUnit {

        val record = tradingProfiles.getRecord(profileReviewId.profileId)

        record.trades.deleteReview(profileReviewId.reviewId)
    }
}
