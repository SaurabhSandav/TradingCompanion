package ui.charts.model

internal sealed class ChartsEvent {

    data class ChangeSymbol(val newSymbol: String) : ChartsEvent()

    data class ChangeTimeframe(val newTimeframe: String) : ChartsEvent()
}
