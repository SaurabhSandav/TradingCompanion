package ui.trades.model

internal sealed class TradesEvent {

    data class OpenChart(val id: Long) : TradesEvent()
}
