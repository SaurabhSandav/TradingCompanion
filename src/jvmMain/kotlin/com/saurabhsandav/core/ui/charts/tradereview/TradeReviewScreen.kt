package com.saurabhsandav.core.ui.charts.tradereview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.LocalAppModule
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.model.TradeId
import com.saurabhsandav.core.ui.TradeContentLauncher
import com.saurabhsandav.core.ui.charts.ChartMarkersProvider
import com.saurabhsandav.core.ui.charts.tradereview.model.TradeReviewEvent.*
import com.saurabhsandav.core.ui.charts.tradereview.model.TradeReviewState.TradeEntry
import com.saurabhsandav.core.ui.charts.tradereview.ui.TradesTable
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.profiles.ProfileSwitcherBox
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
    markersProvider: ChartMarkersProvider,
    tradeContentLauncher: TradeContentLauncher,
) {

    val scope = rememberCoroutineScope()
    val appModule = LocalAppModule.current
    val presenter = remember {
        TradeReviewPresenter(scope, appModule, onOpenChart, markersProvider, tradeContentLauncher)
    }
    val state by presenter.state.collectAsState()

    AppWindow(
        onCloseRequest = onCloseRequest,
        title = "Trade Review",
    ) {

        TradeReviewScreen(
            selectedProfileId = state.selectedProfileId,
            onSelectProfile = { id -> state.eventSink(SelectProfile(id)) },
            trades = state.trades,
            onMarkTrade = { id, isMarked -> state.eventSink(MarkTrade(id, isMarked)) },
            onSelectTrade = { id -> state.eventSink(SelectTrade(id)) },
            onOpenDetails = { state.eventSink(OpenDetails(it)) },
        )
    }
}

@Composable
internal fun TradeReviewScreen(
    selectedProfileId: ProfileId?,
    onSelectProfile: (ProfileId) -> Unit,
    trades: ImmutableList<TradeEntry>,
    onMarkTrade: (id: TradeId, isMarked: Boolean) -> Unit,
    onSelectTrade: (id: TradeId) -> Unit,
    onOpenDetails: (id: TradeId) -> Unit,
) {

    Column {

        Row {

            var profileSwitcherExpanded by state { false }

            ProfileSwitcherBox(
                expanded = profileSwitcherExpanded,
                onExpandedChange = { profileSwitcherExpanded = it },
                selectedProfileId = selectedProfileId,
                onSelectProfile = onSelectProfile,
            ) { profileName ->

                TextButton(
                    modifier = Modifier.weight(1F),
                    onClick = { profileSwitcherExpanded = true },
                    content = { Text("Profile: ${profileName ?: "None"}") },
                )
            }
        }

        Divider()

        TradesTable(
            trades = trades,
            onMarkTrade = onMarkTrade,
            onSelectTrade = onSelectTrade,
            onOpenDetails = onOpenDetails,
        )
    }
}
