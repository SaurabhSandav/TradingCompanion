package com.saurabhsandav.core.ui.barreplay.charts.model

import com.saurabhsandav.core.trading.Timeframe

sealed class ReplayChartsEvent {

    object Reset : ReplayChartsEvent()

    object Next : ReplayChartsEvent()

    data class ChangeIsAutoNextEnabled(val isAutoNextEnabled: Boolean) : ReplayChartsEvent()

    data class ChangeTicker(val newTicker: String) : ReplayChartsEvent()

    data class ChangeTimeframe(val newTimeframe: Timeframe) : ReplayChartsEvent()
}
