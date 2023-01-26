package ui.barreplay.charts.model

sealed class ReplayChartsEvent {

    object Reset : ReplayChartsEvent()

    object Next : ReplayChartsEvent()

    data class ChangeIsAutoNextEnabled(val isAutoNextEnabled: Boolean) : ReplayChartsEvent()

    data class ChangeSymbol(val newSymbol: String) : ReplayChartsEvent()

    data class ChangeTimeframe(val newTimeframe: String) : ReplayChartsEvent()
}
