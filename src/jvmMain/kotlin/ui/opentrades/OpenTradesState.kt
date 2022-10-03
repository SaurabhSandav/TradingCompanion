package ui.opentrades

import androidx.compose.runtime.Immutable
import ui.common.form.SingleSelectionState
import ui.common.form.SwitchState
import ui.common.form.TextFieldState

@Immutable
internal data class OpenTradesState(
    val openTrades: List<OpenTradeListEntry>,
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

class OpenTradeFormState {

    val ticker = SingleSelectionState(
        labelText = "Select Stock...",
    )

    val quantity = TextFieldState(
        initialValue = "",
        isErrorCheck = { it.isEmpty() || it.toBigDecimalOrNull() == null },
        onValueChange = { _, new -> new.trim() }
    )

    val isLong = SwitchState(false)

    val entry = TextFieldState(
        initialValue = "",
        isErrorCheck = { it.isEmpty() || it.toBigDecimalOrNull() == null },
        onValueChange = { _, new -> new.trim() }
    )

    val stop = TextFieldState(
        initialValue = "",
        isErrorCheck = { it.isEmpty() || it.toBigDecimalOrNull() == null },
        onValueChange = { _, new -> new.trim() }
    )

    val target = TextFieldState(
        initialValue = "",
        isErrorCheck = { it.isEmpty() || it.toBigDecimalOrNull() == null },
        onValueChange = { _, new -> new.trim() }
    )

    // Calls isValid() on all states and updates the error states if not up to date
    fun canSubmit() = listOf(ticker, quantity, isLong, entry, stop, target).map { it.isValid() }.all { it }
}
