package ui.sizing

sealed class SizingEvent {

    data class AddTrade(val ticker: String) : SizingEvent()

    data class UpdateTradeEntry(val id: Long, val entry: String) : SizingEvent()

    data class UpdateTradeStop(val id: Long, val stop: String) : SizingEvent()

    data class RemoveTrade(val id: Long) : SizingEvent()
}
