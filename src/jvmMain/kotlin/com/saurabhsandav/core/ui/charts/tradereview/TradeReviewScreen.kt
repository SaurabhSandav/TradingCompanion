package com.saurabhsandav.core.ui.charts.tradereview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.LocalAppModule
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.charts.ChartMarkersProvider
import com.saurabhsandav.core.ui.charts.tradereview.model.TradeReviewEvent.*
import com.saurabhsandav.core.ui.charts.tradereview.model.TradeReviewState.*
import com.saurabhsandav.core.ui.charts.tradereview.ui.MainTabRow
import com.saurabhsandav.core.ui.charts.tradereview.ui.MarkedTradesTable
import com.saurabhsandav.core.ui.charts.tradereview.ui.ProfileTradesTable
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import com.saurabhsandav.core.ui.tradecontent.TradeContentLauncher
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
        appModule.tradeReviewModule(scope).presenter(onOpenChart, markersProvider, tradeContentLauncher)
    }
    val state by presenter.state.collectAsState()

    AppWindow(
        onCloseRequest = onCloseRequest,
        title = "Trade Review",
    ) {

        TradeReviewScreen(
            selectedProfileId = state.selectedProfileId,
            onSelectProfile = { profileTradeId -> state.eventSink(SelectProfile(profileTradeId)) },
            trades = state.trades,
            markedTrades = state.markedTrades,
            onMarkTrade = { profileTradeId, isMarked -> state.eventSink(MarkTrade(profileTradeId, isMarked)) },
            onSelectTrade = { profileTradeId -> state.eventSink(SelectTrade(profileTradeId)) },
            onOpenDetails = { state.eventSink(OpenDetails(it)) },
            onClearMarkedTrades = { state.eventSink(ClearMarkedTrades) },
        )
    }
}

@Composable
internal fun TradeReviewScreen(
    selectedProfileId: ProfileId?,
    onSelectProfile: (ProfileId) -> Unit,
    trades: ImmutableList<TradeEntry>,
    markedTrades: ImmutableList<MarkedTradeEntry>,
    onMarkTrade: (profileTradeId: ProfileTradeId, isMarked: Boolean) -> Unit,
    onSelectTrade: (profileTradeId: ProfileTradeId) -> Unit,
    onOpenDetails: (profileTradeId: ProfileTradeId) -> Unit,
    onClearMarkedTrades: () -> Unit,
) {

    var selectedTab by state { Tab.Profile }

    Scaffold(
        topBar = {

            MainTabRow(
                selectedTab = selectedTab,
                onSelectTab = { selectedTab = it },
                selectedProfileId = selectedProfileId,
                onSelectProfile = onSelectProfile,
            )
        },
        floatingActionButton = {

            AnimatedVisibility(
                visible = selectedTab == Tab.Marked && markedTrades.isNotEmpty(),
                enter = scaleIn(),
                exit = scaleOut(),
            ) {

                FloatingActionButton(onClick = onClearMarkedTrades) {
                    Icon(Icons.Default.ClearAll, contentDescription = "Clear marked trades")
                }
            }
        },
    ) { paddingValues ->

        Crossfade(selectedTab, Modifier.padding(paddingValues)) { tab ->

            when (tab) {
                Tab.Profile -> ProfileTradesTable(
                    trades = trades,
                    onMarkTrade = onMarkTrade,
                    onSelectTrade = onSelectTrade,
                    onOpenDetails = onOpenDetails,
                )

                Tab.Marked -> MarkedTradesTable(
                    markedTrades = markedTrades,
                    onUnMarkTrade = { profileTradeId -> onMarkTrade(profileTradeId, false) },
                    onSelectTrade = onSelectTrade,
                    onOpenDetails = onOpenDetails,
                )
            }
        }
    }
}
