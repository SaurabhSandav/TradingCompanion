package com.saurabhsandav.core.ui.trades

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.model.TradeFilter
import com.saurabhsandav.core.trades.model.TradeId
import com.saurabhsandav.core.ui.common.*
import com.saurabhsandav.core.ui.common.app.WindowTitle
import com.saurabhsandav.core.ui.trades.model.TradesState.TradesList
import com.saurabhsandav.core.ui.trades.ui.TradesTable
import com.saurabhsandav.core.ui.tradesfiltersheet.TradesFilterSheet
import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun TradesScreen(
    profileId: ProfileId,
    tradesList: TradesList,
    onOpenDetails: (TradeId) -> Unit,
    onOpenChart: (TradeId) -> Unit,
    onSetFocusModeEnabled: (Boolean) -> Unit,
    onApplyFilter: (TradeFilter) -> Unit,
    errors: ImmutableList<UIErrorMessage>,
) {

    // Set window title
    WindowTitle("Trades")

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    }
                )
            },
            onDismissSheet = onDismissSheet,
        ) {

            TradesTable(
                tradesList = tradesList,
                onOpenDetails = onOpenDetails,
                onOpenChart = onOpenChart,
                onSetFocusModeEnabled = { isEnabled ->
                    onSetFocusModeEnabled(isEnabled)
                    if (isEnabled) filterConfig = FilterConfig()
                },
                onFilter = { sheetState = SideSheetState.Open },
            )
        }

        // Errors
        errors.forEach { errorMessage ->

            ErrorSnackbar(snackbarHostState, errorMessage)
        }
    }
}
