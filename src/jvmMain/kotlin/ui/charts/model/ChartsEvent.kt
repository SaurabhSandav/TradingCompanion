package ui.charts.model

import trading.Timeframe

internal sealed class ChartsEvent {

    data class ChangeSymbol(val newSymbol: String) : ChartsEvent()

    data class ChangeTimeframe(val newTimeframe: Timeframe) : ChartsEvent()
}
