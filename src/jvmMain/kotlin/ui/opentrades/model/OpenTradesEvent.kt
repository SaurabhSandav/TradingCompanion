package ui.opentrades.model

internal sealed class OpenTradesEvent {

    object AddTrade : OpenTradesEvent()

    data class EditTrade(val id: Int) : OpenTradesEvent()

    data class CloseTrade(val id: Int) : OpenTradesEvent()

    data class DeleteTrade(val id: Int) : OpenTradesEvent()
}
