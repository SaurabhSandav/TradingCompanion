package ui.barreplay.charts.model

sealed class ReplayChartsEvent {

    object Reset : ReplayChartsEvent()

    object Next : ReplayChartsEvent()

    data class ChangeIsAutoNextEnabled(val isAutoNextEnabled: Boolean) : ReplayChartsEvent()

    object NewChart : ReplayChartsEvent()

    data class CloseChart(val id: Int) : ReplayChartsEvent()

    data class SelectChart(val id: Int) : ReplayChartsEvent()

    object NextChart : ReplayChartsEvent()

    object PreviousChart : ReplayChartsEvent()

    data class ChangeSymbol(val newSymbol: String) : ReplayChartsEvent()

    data class ChangeTimeframe(val newTimeframe: String) : ReplayChartsEvent()
}
