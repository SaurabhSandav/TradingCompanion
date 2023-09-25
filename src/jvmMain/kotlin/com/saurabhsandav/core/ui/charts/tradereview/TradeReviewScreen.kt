package com.saurabhsandav.core.ui.charts.tradereview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Divider
import androidx.compose.runtime.*
import com.saurabhsandav.core.LocalAppModule
import com.saurabhsandav.core.ui.charts.tradereview.model.TradeReviewEvent.*
import com.saurabhsandav.core.ui.charts.tradereview.model.TradeReviewState.TradesByDay
import com.saurabhsandav.core.ui.charts.tradereview.ui.TradesTable
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.app.AppWindowsManager
import com.saurabhsandav.core.ui.landing.model.LandingState.TradeWindowParams
import com.saurabhsandav.core.ui.profiles.ProfileSwitcher
import kotlinx.collections.immutable.ImmutableList
import kotlinx.datetime.Instant

@Composable
internal fun TradeReviewWindow(
    onCloseRequest: () -> Unit,
    onOpenChart: (
        ticker: String,
        start: Instant,
        end: Instant?,
    ) -> Unit,
    markersProvider: TradeReviewMarkersProvider,
    tradeWindowsManager: AppWindowsManager<TradeWindowParams>,
) {

    val scope = rememberCoroutineScope()
    val appModule = LocalAppModule.current
    val presenter = remember {
        TradeReviewPresenter(scope, appModule, onOpenChart, markersProvider, tradeWindowsManager)
    }
    val state by presenter.state.collectAsState()

    AppWindow(
        onCloseRequest = onCloseRequest,
        title = "Trade Review",
    ) {

        TradeReviewScreen(
            selectedProfileId = state.selectedProfileId,
            onSelectProfile = { id -> state.eventSink(SelectProfile(id)) },
            tradesByDays = state.tradesByDays,
            onMarkTrade = { id, isMarked -> state.eventSink(MarkTrade(id, isMarked)) },
            onSelectTrade = { id -> state.eventSink(SelectTrade(id)) },
            onOpenDetails = { state.eventSink(OpenDetails(it)) },
        )
    }
}

@Composable
internal fun TradeReviewScreen(
    selectedProfileId: Long?,
    onSelectProfile: (Long) -> Unit,
    tradesByDays: ImmutableList<TradesByDay>,
    onMarkTrade: (id: Long, isMarked: Boolean) -> Unit,
    onSelectTrade: (id: Long) -> Unit,
    onOpenDetails: (id: Long) -> Unit,
) {

    Column {

        Row {

            ProfileSwitcher(
                selectedProfileId = selectedProfileId,
                onSelectProfile = onSelectProfile,
            )
        }

        Divider()

        TradesTable(
            tradesByDays = tradesByDays,
            onMarkTrade = onMarkTrade,
            onSelectTrade = onSelectTrade,
            onOpenDetails = onOpenDetails,
        )
    }
}
