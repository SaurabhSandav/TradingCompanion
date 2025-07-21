package com.saurabhsandav.core.ui.review

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.LocalAppGraph
import com.saurabhsandav.core.ui.common.ConfirmationDialog
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.review.model.ReviewEvent.OpenChart
import com.saurabhsandav.core.ui.review.model.ReviewEvent.OpenDetails
import com.saurabhsandav.core.ui.review.model.ReviewEvent.OpenMarkdownLink
import com.saurabhsandav.core.ui.review.model.ReviewEvent.SaveReview
import com.saurabhsandav.core.ui.review.model.ReviewEvent.SetTitle
import com.saurabhsandav.core.ui.review.model.ReviewState.Tab
import com.saurabhsandav.core.ui.review.model.ReviewState.TradeEntry
import com.saurabhsandav.core.ui.review.ui.ReviewEditable
import com.saurabhsandav.core.ui.review.ui.ReviewTopAppBar
import com.saurabhsandav.core.ui.review.ui.TradesTable
import com.saurabhsandav.core.ui.tradecontent.ProfileReviewId
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId

@Composable
internal fun ReviewWindow(
    profileReviewId: ProfileReviewId,
    onCloseRequest: () -> Unit,
) {

    val scope = rememberCoroutineScope()
    val appGraph = LocalAppGraph.current
    val graph = remember { appGraph.reviewGraphFactory.create(profileReviewId) }
    val presenter = remember { graph.presenterFactory.create(scope) }
    val state = presenter.state.collectAsState().value ?: return

    var showCloseConfirmationDialog by state { false }
    var reviewInEdit by state { state.review }
    val canSaveReview = state.review != reviewInEdit

    AppWindow(
        onCloseRequest = {
            when {
                canSaveReview -> showCloseConfirmationDialog = true
                else -> onCloseRequest()
            }
        },
        title = state.title,
    ) {

        ReviewScreen(
            title = state.title,
            onSetTitle = { title -> state.eventSink(SetTitle(title)) },
            review = reviewInEdit,
            onReviewChange = { review -> reviewInEdit = review },
            canSaveReview = canSaveReview,
            onSaveReview = { state.eventSink(SaveReview(reviewInEdit)) },
            onMarkdownLinkClicked = { linkText -> state.eventSink(OpenMarkdownLink(linkText)) },
            trades = state.trades,
            onOpenChart = { id -> state.eventSink(OpenChart(id)) },
            onOpenDetails = { id -> state.eventSink(OpenDetails(id)) },
        )

        if (showCloseConfirmationDialog) {

            ConfirmationDialog(
                text = "Do you want to discard unsaved changes and close?",
                onDismiss = { showCloseConfirmationDialog = false },
                onConfirm = onCloseRequest,
            )
        }
    }
}

@Composable
private fun ReviewScreen(
    title: String,
    onSetTitle: (String) -> Unit,
    review: String,
    onReviewChange: (String) -> Unit,
    canSaveReview: Boolean,
    onSaveReview: () -> Unit,
    onMarkdownLinkClicked: (String) -> Unit,
    trades: List<TradeEntry>,
    onOpenChart: (profileTradeId: ProfileTradeId) -> Unit,
    onOpenDetails: (profileTradeId: ProfileTradeId) -> Unit,
) {

    var selectedTab by state { Tab.Review }
    var isReviewEditMode by state { review.isEmpty() }

    Scaffold(
        topBar = {

            ReviewTopAppBar(
                title = title,
                onSetTitle = onSetTitle,
                isReviewEditMode = isReviewEditMode,
                onToggleEditReview = { isReviewEditMode = !isReviewEditMode },
                canSaveReview = canSaveReview,
                onSaveReview = onSaveReview,
            )
        },
    ) { paddingValues ->

        Column(Modifier.fillMaxSize().padding(paddingValues)) {

            TabRow(
                selectedTabIndex = if (selectedTab == Tab.Review) 0 else 1,
            ) {

                Tab(
                    selected = selectedTab == Tab.Review,
                    onClick = { selectedTab = Tab.Review },
                    text = { Text("Review") },
                )

                Tab(
                    selected = selectedTab == Tab.Trades,
                    onClick = { selectedTab = Tab.Trades },
                    text = { Text("Trades") },
                )
            }

            val saveableStateHolder = rememberSaveableStateHolder()

            Crossfade(selectedTab) { tab ->

                saveableStateHolder.SaveableStateProvider(tab) {

                    when (tab) {
                        Tab.Review -> ReviewEditable(
                            edit = isReviewEditMode,
                            review = review,
                            onReviewChange = onReviewChange,
                            onSaveReview = onSaveReview,
                            onMarkdownLinkClicked = onMarkdownLinkClicked,
                        )

                        Tab.Trades -> TradesTable(
                            trades = trades,
                            onOpenDetails = onOpenDetails,
                            onOpenChart = onOpenChart,
                        )
                    }
                }
            }
        }
    }
}
