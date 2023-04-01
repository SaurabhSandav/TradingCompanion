package com.saurabhsandav.core.ui.barreplay.charts.model

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.ui.stockchart.StockChart
import com.saurabhsandav.core.ui.stockchart.StockChartsState
import com.saurabhsandav.core.ui.tradeorderform.model.OrderFormType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.util.*

@Immutable
internal data class ReplayChartsState(
    val chartsState: StockChartsState,
    val orderFormParams: ImmutableList<OrderFormParams>,
    val chartInfo: (StockChart) -> ReplayChartInfo = { ReplayChartInfo() },
) {

    @Immutable
    internal data class OrderFormParams(
        val id: UUID,
        val formType: OrderFormType,
    )

    @Immutable
    internal data class ReplayChartInfo(
        val replayTime: Flow<String> = emptyFlow(),
    )
}
