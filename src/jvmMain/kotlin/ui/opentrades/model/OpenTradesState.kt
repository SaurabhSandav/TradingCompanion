package ui.opentrades.model

import androidx.compose.runtime.Immutable
import ui.addopentrade.AddOpenTradeFormState

@Immutable
internal data class OpenTradesState(
    val openTrades: List<OpenTradeListEntry>,
    val addTradeWindowState: AddTradeWindowState,
    val closeTradeWindowState: CloseTradeWindowState,
)

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

internal sealed class AddTradeWindowState {

    data class Open(val formState: AddOpenTradeFormState.Model? = null) : AddTradeWindowState()

    object Closed : AddTradeWindowState()
}

internal sealed class CloseTradeWindowState {

    data class Open(val formState: AddOpenTradeFormState.Model) : CloseTradeWindowState()

    object Closed : CloseTradeWindowState()
}
