package com.saurabhsandav.core.ui.tradereview

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.window.WindowPlacement
import com.saurabhsandav.core.LocalAppModule
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.model.TradeFilter
import com.saurabhsandav.core.ui.charts.ChartsHandle
import com.saurabhsandav.core.ui.common.SideSheetHost
import com.saurabhsandav.core.ui.common.SideSheetState
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.app.rememberAppWindowState
import com.saurabhsandav.core.ui.common.saveableState
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewEvent.*
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewState.*
import com.saurabhsandav.core.ui.tradereview.ui.MainTabRow
import com.saurabhsandav.core.ui.tradereview.ui.TradesTableSwitcher
import com.saurabhsandav.core.ui.tradesfiltersheet.TradesFilterSheet
import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun TradeReviewWindow(
    onCloseRequest: () -> Unit,
    chartsHandle: ChartsHandle,
    tradeReviewHandle: TradeReviewHandle,
) {

    val scope = rememberCoroutineScope()
    val appModule = LocalAppModule.current
    val presenter = remember { appModule.tradeReviewModule(scope).presenter(chartsHandle) }
    val state by presenter.state.collectAsState()

    LaunchedEffect(state.eventSink) {
        tradeReviewHandle.events.collect(state.eventSink)
    }

    AppWindow(
        state = rememberAppWindowState(
            preferredPlacement = WindowPlacement.Floating,
            forcePreferredPlacement = true,
        ),
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
            onApplyFilter = { state.eventSink(ApplyFilter(it)) },
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
    onApplyFilter: (TradeFilter) -> Unit,
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

        var sheetState by state { SideSheetState.Closed }
        val onDismissSheet = { sheetState = SideSheetState.Closed }
        var filterConfig by saveableState(stateSaver = FilterConfig.Saver) { FilterConfig() }

        SideSheetHost(
            modifier = Modifier.padding(paddingValues),
            sheetState = sheetState,
            sheet = {

                TradesFilterSheet(
                    filter = filterConfig,
                    onFilterChange = {
                        filterConfig = it
                        onApplyFilter(it.toTradeFilter())
                        onDismissSheet()
                    }
                )
            },
            onDismissSheet = onDismissSheet,
        ) {

            TradesTableSwitcher(
                selectedTab = selectedTab,
                trades = trades,
                markedTrades = markedTrades,
                onMarkTrade = onMarkTrade,
                onSelectTrade = onSelectTrade,
                onOpenDetails = onOpenDetails,
                onFilter = { sheetState = SideSheetState.Open },
            )
        }
    }
}
