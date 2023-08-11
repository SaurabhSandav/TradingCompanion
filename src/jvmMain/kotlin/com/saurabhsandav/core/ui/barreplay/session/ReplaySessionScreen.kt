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
            onSelectProfile = { id -> presenter.event(SelectProfile(id)) },
            onResetReplay = { presenter.event(ResetReplay) },
            onAdvanceReplay = { presenter.event(AdvanceReplay) },
            enableAdvanceReplayByBar = state.enableAdvanceReplayByBar,
            onAdvanceReplayByBar = { presenter.event(AdvanceReplayByBar) },
        )

        Divider()

        ReplayOrdersTable(
            replayOrderItems = state.replayOrderItems,
            onCancelOrder = { id -> presenter.event(CancelOrder(id)) },
        )
    }

    ReplayCharts(
        onCloseRequest = onNewReplay,
        chartsState = state.chartsState,
        chartInfo = state.chartInfo,
        onAdvanceReplay = { presenter.event(AdvanceReplay) },
        enableAdvanceReplayByBar = state.enableAdvanceReplayByBar,
        onAdvanceReplayByBar = { presenter.event(AdvanceReplayByBar) },
        onIsAutoNextEnabledChange = { presenter.event(SetIsAutoNextEnabled(it)) },
        isTradingEnabled = state.selectedProfileId != null,
        onBuy = { stockChart -> presenter.event(Buy(stockChart)) },
        onSell = { stockChart -> presenter.event(Sell(stockChart)) },
    )

    // Order form windows
    state.orderFormWindowsManager.Windows { window ->

        ReplayOrderFormWindow(
            replayOrdersManager = presenter.replayOrdersManager,
            initialFormModel = window.params.initialFormModel,
            onCloseRequest = window::close,
        )
    }
}
