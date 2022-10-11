package ui.closedtrades.model

import ui.addclosedtrade.CloseTradeFormFields

internal sealed class ClosedTradesEvent {

    data class DeleteTrade(val id: Int) : ClosedTradesEvent()

    sealed class DeleteConfirmationDialog : ClosedTradesEvent() {

        data class Confirm(val id: Int) : DeleteConfirmationDialog()

        object Dismiss : DeleteConfirmationDialog()
    }

    sealed class EditTradeWindow : ClosedTradesEvent() {

        data class Open(val id: Int) : EditTradeWindow()

        data class SaveTrade(
            val model: CloseTradeFormFields.Model,
        ) : EditTradeWindow()

        object Close : EditTradeWindow()
    }
}
