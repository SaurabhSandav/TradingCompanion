package com.saurabhsandav.core.ui.barreplay.charts

import androidx.compose.runtime.*
import com.saurabhsandav.core.LocalAppModule
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.ui.barreplay.charts.model.ReplayChartsEvent.*
import com.saurabhsandav.core.ui.barreplay.charts.ui.ReplayCharts
import com.saurabhsandav.core.ui.tradeorderform.OrderFormWindow
import kotlinx.datetime.Instant

@Composable
internal fun ReplayChartsScreen(
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
        ReplayChartsPresenter(
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
        onReset = { presenter.event(Reset) },
        onNext = { presenter.event(Next) },
        onIsAutoNextEnabledChange = { presenter.event(ChangeIsAutoNextEnabled(it)) },
        onBuy = { stockChart -> presenter.event(Buy(stockChart)) },
        onSell = { stockChart -> presenter.event(Sell(stockChart)) },
    )

    // Order form windows
    state.orderFormParams.forEach { params ->

        key(params.id) {

            OrderFormWindow(
                formType = params.formType,
                onCloseRequest = { presenter.event(CloseOrderForm(params.id)) },
            )
        }
    }
}
