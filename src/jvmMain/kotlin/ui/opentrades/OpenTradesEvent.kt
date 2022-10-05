package ui.opentrades

internal sealed class OpenTradesEvent {

    sealed class AddTradeWindow : OpenTradesEvent() {

        object AddTrade : AddTradeWindow()

        data class EditTrade(val id: Int) : AddTradeWindow()

        object Close : AddTradeWindow()
    }

    data class AddNewTrade(
        val model: AddOpenTradeFormState.Model,
    ) : OpenTradesEvent()
}
