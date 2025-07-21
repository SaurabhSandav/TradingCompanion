package com.saurabhsandav.core.ui.reviews

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.saurabhsandav.core.ui.landing.LandingGraph
import com.saurabhsandav.core.ui.landing.LandingSwitcherItem
import com.saurabhsandav.core.ui.landing.LandingSwitcherItemKey
import com.saurabhsandav.core.ui.landing.model.LandingScreen
import com.saurabhsandav.core.ui.reviews.model.ReviewsEvent.DeleteReview
import com.saurabhsandav.core.ui.reviews.model.ReviewsEvent.NewReview
import com.saurabhsandav.core.ui.reviews.model.ReviewsEvent.OpenReview
import com.saurabhsandav.core.ui.reviews.model.ReviewsEvent.TogglePinReview
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope

@LandingSwitcherItemKey(LandingScreen.Reviews)
@ContributesIntoMap(LandingGraph::class)
@Inject
internal class ReviewsLandingSwitcherItem(
    landingGraph: LandingGraph,
    coroutineScope: CoroutineScope,
) : LandingSwitcherItem {

    private val graph = landingGraph.reviewGraphFactory.create()

    private val presenter = graph.presenterFactory.create(coroutineScope)

    @Composable
    override fun Content() {

        val state by presenter.state.collectAsState()

        ReviewsScreen(
            reviewEntries = state.reviewEntries,
            onNewReview = { state.eventSink(NewReview) },
            onOpenReview = { id -> state.eventSink(OpenReview(id)) },
            onTogglePinReview = { id -> state.eventSink(TogglePinReview(id)) },
            onDeleteReview = { id -> state.eventSink(DeleteReview(id)) },
        )
    }
}
