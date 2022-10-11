package ui.opentrades.model

import androidx.compose.runtime.Immutable
import ui.addclosedtrade.CloseTradeFormFields
import ui.addopentrade.AddOpenTradeFormFields

@Immutable
internal data class OpenTradesState(
    val openTrades: List<OpenTradeListEntry>,
    val addTradeWindowState: AddTradeWindow,
    val closeTradeWindowState: CloseTradeWindow,
) {

    internal sealed class AddTradeWindow {

        data class Open(val formModel: AddOpenTradeFormFields.Model) : AddTradeWindow()

        object Closed : AddTradeWindow()
    }

    internal sealed class CloseTradeWindow {

        data class Open(val formModel: CloseTradeFormFields.Model) : CloseTradeWindow()

        object Closed : CloseTradeWindow()
    }
}

internal data class OpenTradeListEntry(
    val id: Int,
    val broker: String,
    val ticker: String,
    val instrument: String,
    val quantity: String,
    val side: String,
    val entry: String,
    val stop: String,
    val entryTime: String,
    val target: String,
)
