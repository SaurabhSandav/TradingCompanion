package ui.closedtrades.model

import ui.addclosedtradedetailed.CloseTradeDetailedFormFields

internal sealed class ClosedTradesEvent {

    data class DeleteTrade(val id: Long) : ClosedTradesEvent()

    sealed class DeleteConfirmationDialog : ClosedTradesEvent() {

        data class Confirm(val id: Long) : DeleteConfirmationDialog()

        object Dismiss : DeleteConfirmationDialog()
    }

    data class OpenChart(val id: Long) : ClosedTradesEvent()

    data class EditTrade(val id: Long) : ClosedTradesEvent()

    data class SaveTrade(
        val model: CloseTradeDetailedFormFields.Model,
    ) : ClosedTradesEvent()
}
