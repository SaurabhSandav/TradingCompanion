package com.saurabhsandav.core.ui.barreplay.session

import androidx.compose.runtime.*
import com.saurabhsandav.core.LocalAppModule
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.ui.barreplay.session.model.ReplaySessionEvent.*
import com.saurabhsandav.core.ui.barreplay.session.ui.ReplayCharts
import com.saurabhsandav.core.ui.tradeorderform.OrderFormWindow
import kotlinx.datetime.Instant

@Composable
internal fun ReplaySessionScreen(
    onNewReplay: () -> Unit,
    baseTimeframe: Timeframe,
    candlesBefore: Int,
    replayFrom: Instant,
    dataTo: Instant,
    replayFullBar: Boolean,
    initialTicker: String,
) {

    val scope = rememberCoroutineScope()
    val appModule = LocalAppModule.current
    val presenter = remember {
        ReplaySessionPresenter(
            coroutineScope = scope,
            baseTimeframe = baseTimeframe,
            candlesBefore = candlesBefore,
            replayFrom = replayFrom,
            dataTo = dataTo,
            replayFullBar = replayFullBar,
            initialTicker = initialTicker,
            appModule = appModule
        )
    }
    val state by presenter.state.collectAsState()

    ReplayCharts(
        onNewReplay = onNewReplay,
        chartsState = state.chartsState,
        chartInfo = state.chartInfo,
        onResetReplay = { presenter.event(ResetReplay) },
        onAdvanceReplay = { presenter.event(AdvanceReplay) },
        onIsAutoNextEnabledChange = { presenter.event(SetIsAutoNextEnabled(it)) },
        selectedProfileId = state.selectedProfileId,
        onSelectProfile = { id -> presenter.event(SelectProfile(id)) },
        onBuy = { stockChart -> presenter.event(Buy(stockChart)) },
        onSell = { stockChart -> presenter.event(Sell(stockChart)) },
    )

    // Order form windows
    state.orderFormParams.forEach { params ->

        key(params.id) {

            OrderFormWindow(
                profileId = params.profileId,
                formType = params.formType,
                onCloseRequest = { presenter.event(CloseOrderForm(params.id)) },
            )
        }
    }
}
