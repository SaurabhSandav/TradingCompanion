package com.saurabhsandav.core.ui.barreplay.charts.model

import com.saurabhsandav.core.ui.stockchart.StockChart
import java.util.*

internal sealed class ReplayChartsEvent {

    object ResetReplay : ReplayChartsEvent()

    object AdvanceReplay : ReplayChartsEvent()

    data class SetIsAutoNextEnabled(val isAutoNextEnabled: Boolean) : ReplayChartsEvent()

    data class SelectProfile(val id: Long) : ReplayChartsEvent()

    data class Buy(val stockChart: StockChart) : ReplayChartsEvent()

    data class Sell(val stockChart: StockChart) : ReplayChartsEvent()

    data class CloseOrderForm(val id: UUID) : ReplayChartsEvent()
}
