package com.saurabhsandav.core.ui.review

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.LocalAppModule
import com.saurabhsandav.core.ui.common.ConfirmationDialog
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.review.model.ReviewEvent.*
import com.saurabhsandav.core.ui.review.model.ReviewState.Tab
import com.saurabhsandav.core.ui.review.model.ReviewState.TradeEntry
import com.saurabhsandav.core.ui.review.ui.ReviewEditable
import com.saurabhsandav.core.ui.review.ui.ReviewTopAppBar
import com.saurabhsandav.core.ui.review.ui.TradesTable
import com.saurabhsandav.core.ui.tradecontent.ProfileReviewId
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun ReviewWindow(
    profileReviewId: ProfileReviewId,
    onCloseRequest: () -> Unit,
) {

    val scope = rememberCoroutineScope()
    val appModule = LocalAppModule.current
    val reviewModule = remember { appModule.reviewModule(scope, profileReviewId) }
    val presenter = remember { reviewModule.presenter() }
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
            isMarkdown = state.isMarkdown,
            onToggleMarkdown = { state.eventSink(ToggleMarkdown) },
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
    isMarkdown: Boolean,
    onToggleMarkdown: () -> Unit,
    review: String,
    onReviewChange: (String) -> Unit,
    canSaveReview: Boolean,
    onSaveReview: () -> Unit,
    onMarkdownLinkClicked: (String) -> Unit,
    trades: ImmutableList<TradeEntry>,
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
                isMarkdown = isMarkdown,
                onToggleMarkdown = onToggleMarkdown,
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
                            isMarkdown = isMarkdown,
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
