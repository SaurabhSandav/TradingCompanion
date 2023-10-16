package com.saurabhsandav.core.ui.barreplay.session

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Divider
import androidx.compose.runtime.*
import com.saurabhsandav.core.LocalAppModule
import com.saurabhsandav.core.ui.barreplay.model.BarReplayState.ReplayParams
import com.saurabhsandav.core.ui.barreplay.session.model.ReplaySessionEvent.*
import com.saurabhsandav.core.ui.barreplay.session.replayorderform.ReplayOrderFormWindow
import com.saurabhsandav.core.ui.barreplay.session.ui.ReplayCharts
import com.saurabhsandav.core.ui.barreplay.session.ui.ReplayConfigRow
import com.saurabhsandav.core.ui.barreplay.session.ui.ReplayOrdersTable

@Composable
internal fun ReplaySessionScreen(
    onNewReplay: () -> Unit,
    replayParams: ReplayParams,
) {

    val scope = rememberCoroutineScope()
    val appModule = LocalAppModule.current
    val presenter = remember {
        ReplaySessionPresenter(
            coroutineScope = scope,
            replayParams = replayParams,
            appModule = appModule
        )
    }
    val state by presenter.state.collectAsState()

    Column {

        ReplayConfigRow(
            onNewReplay = onNewReplay,
            selectedProfileId = state.selectedProfileId,
            onSelectProfile = { id -> state.eventSink(SelectProfile(id)) },
            onResetReplay = { state.eventSink(ResetReplay) },
            replayFullBar = replayParams.replayFullBar,
            onAdvanceReplay = { state.eventSink(AdvanceReplay) },
            onAdvanceReplayByBar = { state.eventSink(AdvanceReplayByBar) },
        )

        Divider()

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
        onIsAutoNextEnabledChange = { state.eventSink(SetIsAutoNextEnabled(it)) },
        isTradingEnabled = state.selectedProfileId != null,
        onBuy = { stockChart -> state.eventSink(Buy(stockChart)) },
        onSell = { stockChart -> state.eventSink(Sell(stockChart)) },
    )

    // Order form windows
    state.orderFormWindowsManager.Windows { window ->

        ReplayOrderFormWindow(
            replayOrdersManager = presenter.replayOrdersManager,
            initialModel = window.params.initialModel,
            onCloseRequest = window::close,
        )
    }
}
