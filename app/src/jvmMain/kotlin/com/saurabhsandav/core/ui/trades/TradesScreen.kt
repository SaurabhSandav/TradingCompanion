package com.saurabhsandav.core.ui.trades

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.paging.PagingData
import com.saurabhsandav.core.trading.ProfileId
import com.saurabhsandav.core.ui.common.SelectionManager
import com.saurabhsandav.core.ui.common.SideSheetHost
import com.saurabhsandav.core.ui.common.SideSheetState
import com.saurabhsandav.core.ui.common.app.WindowTitle
import com.saurabhsandav.core.ui.common.saveableState
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.trades.model.TradesState.TradeEntry
import com.saurabhsandav.core.ui.trades.ui.TradesOptionsBar
import com.saurabhsandav.core.ui.trades.ui.TradesSelectionBar
import com.saurabhsandav.core.ui.trades.ui.TradesTable
import com.saurabhsandav.core.ui.tradesfiltersheet.TradesFilterSheet
import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig
import com.saurabhsandav.trading.record.model.TradeFilter
import com.saurabhsandav.trading.record.model.TradeId
import com.saurabhsandav.trading.record.model.TradeTagId
import kotlinx.coroutines.flow.Flow

@Composable
internal fun TradesScreen(
    profileId: ProfileId,
    tradeEntries: Flow<PagingData<TradeEntry>>,
    isFocusModeEnabled: Boolean,
    selectionManager: SelectionManager<TradeId>,
    onOpenDetails: (TradeId) -> Unit,
    onOpenChart: (List<TradeId>) -> Unit,
    onSetFocusModeEnabled: (Boolean) -> Unit,
    onApplyFilter: (TradeFilter) -> Unit,
    onNewExecution: () -> Unit,
    onDeleteTrades: (List<TradeId>) -> Unit,
    onAddTag: (List<TradeId>, TradeTagId) -> Unit,
) {

    // Set window title
    WindowTitle("Trades")

    Column {

        Scaffold(
            modifier = Modifier.weight(1F),
        ) { paddingValues ->

            var sheetState by state { SideSheetState.Closed }
            val onDismissSheet = { sheetState = SideSheetState.Closed }
            var filterConfig by saveableState(stateSaver = FilterConfig.Saver) { FilterConfig() }

            SideSheetHost(
                modifier = Modifier.padding(paddingValues),
                sheetState = sheetState,
                sheet = {

                    TradesFilterSheet(
                        profileId = profileId,
                        filter = filterConfig,
                        onFilterChange = {
                            filterConfig = it
                            onApplyFilter(it.toTradeFilter())
                            onDismissSheet()
                        },
                    )
                },
                onDismissSheet = onDismissSheet,
            ) {

                Column {

                    TradesOptionsBar(
                        isFocusModeEnabled = isFocusModeEnabled,
                        onSetFocusModeEnabled = { isEnabled ->
                            onSetFocusModeEnabled(isEnabled)
                            if (isEnabled) filterConfig = FilterConfig()
                        },
                        onFilter = { sheetState = SideSheetState.Open },
                        onNewExecution = onNewExecution,
                    )

                    HorizontalDivider()

                    TradesTable(
                        tradeEntries = tradeEntries,
                        isMarked = { id -> id in selectionManager.selection },
                        onMarkExecution = selectionManager::select,
                        onOpenDetails = onOpenDetails,
                        onOpenChart = { onOpenChart(listOf(it)) },
                    )
                }
            }
        }

        TradesSelectionBar(
            profileId = profileId,
            selectionManager = selectionManager,
            onDeleteTrades = onDeleteTrades,
            onAddTag = onAddTag,
            onOpenChart = onOpenChart,
        )
    }
}
