package com.saurabhsandav.core.ui.barreplay.session.model

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.ui.barreplay.session.replayorderform.model.ReplayOrderFormModel
import com.saurabhsandav.core.ui.common.app.AppWindowsManager
import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.ui.stockchart.StockChart
import com.saurabhsandav.core.ui.stockchart.StockChartsState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.util.*

@Immutable
internal data class ReplaySessionState(
    val chartsState: StockChartsState,
    val selectedProfileId: Long?,
    val replayOrderItems: ImmutableList<ReplayOrderListItem>,
    val orderFormWindowsManager: AppWindowsManager<OrderFormParams>,
    val chartInfo: (StockChart) -> ReplayChartInfo,
    val eventSink: (ReplaySessionEvent) -> Unit,
) {

    @Immutable
    internal data class ReplayOrderListItem(
        val id: Long,
        val executionType: String,
        val broker: String,
        val ticker: String,
        val quantity: String,
        val side: String,
        val price: String,
        val timestamp: String,
    )

    @Immutable
    internal data class OrderFormParams(
        val id: UUID,
        val initialFormModel: ((FormValidator) -> ReplayOrderFormModel)? = null,
    )

    @Immutable
    internal data class ReplayChartInfo(
        val replayTime: Flow<String> = emptyFlow(),
        val candleState: Flow<String> = emptyFlow(),
    )
}
