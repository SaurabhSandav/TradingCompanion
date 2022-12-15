package ui.charts.model

internal sealed class ChartsEvent {

    object NewChart : ChartsEvent()

    data class CloseChart(val id: Int) : ChartsEvent()

    data class SelectChart(val id: Int) : ChartsEvent()

    object NextChart : ChartsEvent()

    object PreviousChart : ChartsEvent()

    data class ChangeSymbol(val newSymbol: String) : ChartsEvent()

    data class ChangeTimeframe(val newTimeframe: String) : ChartsEvent()
}
