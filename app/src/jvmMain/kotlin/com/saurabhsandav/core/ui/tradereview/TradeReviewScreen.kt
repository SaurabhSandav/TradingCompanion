package com.saurabhsandav.core.ui.tradereview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.WindowPlacement
import androidx.paging.PagingData
import com.saurabhsandav.core.LocalAppGraph
import com.saurabhsandav.core.trading.ProfileId
import com.saurabhsandav.core.ui.charts.ChartsHandle
import com.saurabhsandav.core.ui.common.SideSheetHost
import com.saurabhsandav.core.ui.common.SideSheetState
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.app.rememberAppWindowState
import com.saurabhsandav.core.ui.common.saveableState
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewEvent.ApplyFilter
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewEvent.ClearMarkedTrades
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewEvent.MarkAllTrades
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewEvent.MarkTrade
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewEvent.OpenDetails
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewEvent.ProfileSelected
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewEvent.SelectTrade
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewState.MarkedTradeItem
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewState.Tab
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewState.TradeItem
import com.saurabhsandav.core.ui.tradereview.ui.MainTabRow
import com.saurabhsandav.core.ui.tradereview.ui.TradeReviewOptionsBar
import com.saurabhsandav.core.ui.tradereview.ui.TradesTableSwitcher
import com.saurabhsandav.core.ui.tradesfiltersheet.TradesFilterSheet
import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig
import com.saurabhsandav.trading.record.model.TradeFilter
import kotlinx.coroutines.flow.Flow

@Composable
internal fun TradeReviewWindow(
    onCloseRequest: () -> Unit,
    chartsHandle: ChartsHandle,
    tradeReviewHandle: TradeReviewHandle,
) {

    val scope = rememberCoroutineScope()
    val appGraph = LocalAppGraph.current
    val presenter = remember {
        appGraph.tradeReviewGraphFactory
            .create()
            .presenterFactory
            .create(scope, chartsHandle)
    }
    val state by presenter.state.collectAsState()

    LaunchedEffect(state.eventSink) {
        tradeReviewHandle.eventsFlow.collect(state.eventSink)
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
            selectedProfileName = state.selectedProfileName,
            onProfileSelected = { profileTradeId -> state.eventSink(ProfileSelected(profileTradeId)) },
            trades = state.trades,
            markedTrades = state.markedTrades,
            onMarkTrade = { profileTradeId, isMarked -> state.eventSink(MarkTrade(profileTradeId, isMarked)) },
            onSelectTrade = { profileTradeId -> state.eventSink(SelectTrade(profileTradeId)) },
            onOpenDetails = { state.eventSink(OpenDetails(it)) },
            onMarkAllTrades = { state.eventSink(MarkAllTrades) },
            onClearMarkedTrades = { state.eventSink(ClearMarkedTrades) },
            onApplyFilter = { state.eventSink(ApplyFilter(it)) },
        )
    }
}

@Composable
internal fun TradeReviewScreen(
    selectedProfileId: ProfileId?,
    selectedProfileName: String?,
    onProfileSelected: (ProfileId?) -> Unit,
    trades: Flow<PagingData<TradeItem>>,
    markedTrades: List<MarkedTradeItem>?,
    onMarkTrade: (profileTradeId: ProfileTradeId, isMarked: Boolean) -> Unit,
    onSelectTrade: (profileTradeId: ProfileTradeId) -> Unit,
    onOpenDetails: (profileTradeId: ProfileTradeId) -> Unit,
    onMarkAllTrades: () -> Unit,
    onClearMarkedTrades: () -> Unit,
    onApplyFilter: (TradeFilter) -> Unit,
) {

    var selectedTab by state { Tab.Profile }

    Scaffold { paddingValues ->

        var sheetState by state { SideSheetState.Closed }
        val onDismissSheet = { sheetState = SideSheetState.Closed }
        var filterConfig by saveableState(stateSaver = FilterConfig.Saver) { FilterConfig() }

        SideSheetHost(
            sheetState = sheetState,
            sheet = {

                if (selectedProfileId != null) {

                    TradesFilterSheet(
                        profileId = selectedProfileId,
                        filter = filterConfig,
                        onFilterChange = {
                            filterConfig = it
                            onApplyFilter(it.toTradeFilter())
                            onDismissSheet()
                        },
                    )
                }
            },
            onDismissSheet = onDismissSheet,
        ) {

            Column(
                modifier = Modifier.padding(paddingValues),
            ) {

                MainTabRow(
                    selectedTab = selectedTab,
                    onSelectTab = { selectedTab = it },
                    selectedProfileId = selectedProfileId,
                    selectedProfileName = selectedProfileName,
                    onProfileSelected = onProfileSelected,
                )

                TradeReviewOptionsBar(
                    selectedTab = selectedTab,
                    isFilterEnabled = selectedProfileId != null,
                    onFilter = { sheetState = SideSheetState.Open },
                    onMarkAllTrades = onMarkAllTrades,
                    tradesAreMarked = markedTrades?.isNotEmpty() == true,
                    onClearMarkedTrades = onClearMarkedTrades,
                )

                HorizontalDivider()

                TradesTableSwitcher(
                    selectedTab = selectedTab,
                    trades = trades,
                    markedTrades = markedTrades,
                    onMarkTrade = onMarkTrade,
                    onSelectTrade = onSelectTrade,
                    onOpenDetails = onOpenDetails,
                )
            }
        }
    }
}
