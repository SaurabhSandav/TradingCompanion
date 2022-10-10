package ui.opentrades.model

import ui.addclosedtrade.CloseTradeFormFields
import ui.addopentrade.AddOpenTradeFormFields

internal sealed class OpenTradesEvent {

    data class DeleteTrade(val id: Int) : OpenTradesEvent()

    sealed class AddTradeWindow : OpenTradesEvent() {

        object Open : AddTradeWindow()

        data class OpenEdit(val id: Int) : AddTradeWindow()

        data class SaveTrade(
            val model: AddOpenTradeFormFields.Model,
        ) : AddTradeWindow()

        object Close : AddTradeWindow()
    }

    sealed class CloseTradeWindow : OpenTradesEvent() {

        data class Open(val id: Int) : CloseTradeWindow()

        data class SaveTrade(
            val model: CloseTradeFormFields.Model,
        ) : CloseTradeWindow()

        object Close : CloseTradeWindow()
    }
}
