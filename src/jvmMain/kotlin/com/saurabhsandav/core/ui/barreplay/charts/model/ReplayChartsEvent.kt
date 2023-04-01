package com.saurabhsandav.core.ui.barreplay.charts.model

import com.saurabhsandav.core.ui.stockchart.StockChart
import java.util.*

internal sealed class ReplayChartsEvent {

    object Reset : ReplayChartsEvent()

    object Next : ReplayChartsEvent()

    data class ChangeIsAutoNextEnabled(val isAutoNextEnabled: Boolean) : ReplayChartsEvent()

    data class Buy(val stockChart: StockChart) : ReplayChartsEvent()

    data class Sell(val stockChart: StockChart) : ReplayChartsEvent()

    data class CloseOrderForm(val id: UUID) : ReplayChartsEvent()
}
