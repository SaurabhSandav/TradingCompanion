package com.saurabhsandav.core.ui.barreplay.session

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import com.saurabhsandav.core.ui.barreplay.BarReplayModule
import com.saurabhsandav.core.ui.barreplay.model.BarReplayState.ReplayParams
import com.saurabhsandav.core.ui.barreplay.session.model.ReplaySessionEvent.*
import com.saurabhsandav.core.ui.barreplay.session.replayorderform.ReplayOrderFormWindow
import com.saurabhsandav.core.ui.barreplay.session.ui.ReplayCharts
import com.saurabhsandav.core.ui.barreplay.session.ui.ReplayConfigRow
import com.saurabhsandav.core.ui.barreplay.session.ui.ReplayOrdersTable

@Composable
internal fun ReplaySessionScreen(
    barReplayModule: BarReplayModule,
    onOpenProfile: () -> Unit,
    onNewReplay: () -> Unit,
    replayParams: ReplayParams,
) {

    val scope = rememberCoroutineScope()
    val presenter = remember { barReplayModule.replaySessionModule(scope, replayParams).presenter() }
    val state by presenter.state.collectAsState()

    Column {

        ReplayConfigRow(
            profileName = state.profileName,
            onOpenProfile = onOpenProfile,
            onNewReplay = onNewReplay,
            replayFullBar = replayParams.replayFullBar,
            onAdvanceReplay = { state.eventSink(AdvanceReplay) },
            onAdvanceReplayByBar = { state.eventSink(AdvanceReplayByBar) },
        )

        HorizontalDivider()

        ReplayOrdersTable(
            replayOrderItems = state.replayOrderItems,
            onCancelOrder = { id -> state.eventSink(CancelOrder(id)) },
        )
    }

    ReplayCharts(
        onCloseRequest = onNewReplay,
        chartsState = state.chartsState,
        chartInfo = state.chartInfo,
        onAdvanceReplay = { state.eventSink(AdvanceReplay) },
        replayFullBar = replayParams.replayFullBar,
        onAdvanceReplayByBar = { state.eventSink(AdvanceReplayByBar) },
        isAutoNextEnabled = state.isAutoNextEnabled,
        onIsAutoNextEnabledChange = { state.eventSink(SetIsAutoNextEnabled(it)) },
        isTradingEnabled = replayParams.profileId != null,
        onBuy = { stockChart -> state.eventSink(Buy(stockChart)) },
        onSell = { stockChart -> state.eventSink(Sell(stockChart)) },
    )

    // Order form windows
    state.orderFormWindowsManager.Windows { window ->

        ReplayOrderFormWindow(
            replayOrdersManager = presenter.replayOrdersManager,
            stockChartParams = window.params.stockChartParams,
            initialModel = window.params.initialModel,
            onCloseRequest = window::close,
        )
    }
}
