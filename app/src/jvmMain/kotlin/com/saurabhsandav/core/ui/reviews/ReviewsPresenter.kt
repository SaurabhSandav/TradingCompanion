package com.saurabhsandav.core.ui.reviews

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.insertSeparators
import androidx.paging.map
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.trades.Review
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.model.ReviewId
import com.saurabhsandav.core.ui.reviews.model.ReviewsEvent
import com.saurabhsandav.core.ui.reviews.model.ReviewsEvent.DeleteReview
import com.saurabhsandav.core.ui.reviews.model.ReviewsEvent.NewReview
import com.saurabhsandav.core.ui.reviews.model.ReviewsEvent.OpenReview
import com.saurabhsandav.core.ui.reviews.model.ReviewsEvent.TogglePinReview
import com.saurabhsandav.core.ui.reviews.model.ReviewsState
import com.saurabhsandav.core.ui.reviews.model.ReviewsState.ReviewEntry
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

    private val tradingRecord = coroutineScope.async { tradingProfiles.getRecord(profileId) }
    private val reviews = coroutineScope.async { tradingRecord.await().reviews }

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule ReviewsState(
            reviewEntries = getReviewEntries(),
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
    private fun getReviewEntries(): Flow<PagingData<ReviewEntry>> = remember {
        flow {

            val reviews = reviews.await()

            val pager = Pager(
                config = PagingConfig(
                    pageSize = 70,
                    enablePlaceholders = false,
                    maxSize = 300,
                ),
                pagingSourceFactory = reviews::getAllPagingSource,
            )

            pager
                .flow
                .map { pagingData ->

                    @Suppress("UNCHECKED_CAST")
                    pagingData
                        .insertSeparators { before, after ->

                            when {
                                // If before is the last review
                                after == null -> null

                                // If first execution is pinned
                                before == null && after.isPinned -> ReviewEntry.Section(
                                    isPinned = true,
                                    count = reviews.getPinnedCount(),
                                )

                                // If either after is first execution or before is from today
                                // And after is from before today
                                (before == null || before.isPinned) && !after.isPinned -> ReviewEntry.Section(
                                    isPinned = false,
                                    count = reviews.getUnpinnedCount(),
                                )

                                else -> null
                            }
                        }
                        .map { reviewOrEntry ->

                            when (reviewOrEntry) {
                                is Review -> ReviewEntry.Item(
                                    id = reviewOrEntry.id,
                                    title = reviewOrEntry.title,
                                    isPinned = reviewOrEntry.isPinned,
                                )

                                else -> reviewOrEntry
                            }
                        } as PagingData<ReviewEntry>
                }
                .emitInto(this)
        }
    }

    private fun onNewReview() = coroutineScope.launchUnit {

        val reviewId = reviews.await().new(
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

        reviews.await().togglePinned(id)
    }

    private fun onDeleteReview(id: ReviewId) = coroutineScope.launchUnit {

        reviews.await().delete(id)
    }
}
