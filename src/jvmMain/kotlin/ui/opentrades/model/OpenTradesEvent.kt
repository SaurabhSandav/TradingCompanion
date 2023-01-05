package ui.opentrades.model

internal sealed class OpenTradesEvent {

    object AddTrade : OpenTradesEvent()

    data class EditTrade(val id: Long) : OpenTradesEvent()

    data class OpenPNLCalculator(val id: Long) : OpenTradesEvent()

    data class CloseTrade(val id: Long) : OpenTradesEvent()

    data class DeleteTrade(val id: Long) : OpenTradesEvent()
}
