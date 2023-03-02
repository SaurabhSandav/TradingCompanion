package com.saurabhsandav.core.ui.charts.model

import com.saurabhsandav.core.trading.Timeframe

internal sealed class ChartsEvent {

    data class ChangeTicker(val newTicker: String) : ChartsEvent()

    data class ChangeTimeframe(val newTimeframe: Timeframe) : ChartsEvent()
}
