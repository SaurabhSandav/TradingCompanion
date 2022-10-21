package ui.closedtrades.model

import ui.addclosedtradedetailed.CloseTradeDetailedFormFields

internal sealed class ClosedTradesEvent {

    data class DeleteTrade(val id: Int) : ClosedTradesEvent()

    sealed class DeleteConfirmationDialog : ClosedTradesEvent() {

        data class Confirm(val id: Int) : DeleteConfirmationDialog()

        object Dismiss : DeleteConfirmationDialog()
    }

    data class OpenChart(val id: Int) : ClosedTradesEvent()

    data class EditTrade(val id: Int) : ClosedTradesEvent()

    data class SaveTrade(
        val model: CloseTradeDetailedFormFields.Model,
    ) : ClosedTradesEvent()
}
