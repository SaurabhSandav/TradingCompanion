package com.saurabhsandav.core.ui.barreplay.session.model

import com.saurabhsandav.core.trading.backtest.BacktestOrderId
import com.saurabhsandav.core.ui.barreplay.session.replayorderform.model.ReplayOrderFormModel
import com.saurabhsandav.core.ui.common.app.AppWindowsManager
import com.saurabhsandav.core.ui.stockchart.StockChart
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.core.ui.stockchart.StockChartsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.util.*

internal data class ReplaySessionState(
    val chartsState: StockChartsState,
    val profileName: String?,
    val replayOrderItems: List<ReplayOrderListItem>,
    val orderFormWindowsManager: AppWindowsManager<OrderFormParams>,
    val chartInfo: (StockChart) -> ReplayChartInfo,
    val isAutoNextEnabled: Boolean,
    val eventSink: (ReplaySessionEvent) -> Unit,
) {

    internal data class ReplayOrderListItem(
        val id: BacktestOrderId,
        val executionType: String,
        val broker: String,
        val ticker: String,
        val quantity: String,
        val side: String,
        val price: String,
        val timestamp: String,
    )

    internal data class OrderFormParams(
        val id: UUID,
        val stockChartParams: StockChartParams,
        val initialModel: ReplayOrderFormModel.Initial?,
    )

    internal data class ReplayChartInfo(
        val replayTime: Flow<String> = emptyFlow(),
        val candleState: Flow<String> = emptyFlow(),
    )
}
