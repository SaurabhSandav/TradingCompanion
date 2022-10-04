package ui.opentrades

import androidx.compose.runtime.Immutable
import ui.common.form.FormManager
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

    private val manager = FormManager()

    val ticker = manager.singleSelectionState(
        labelText = "Select Stock...",
    )

    val quantity = TextFieldState(
        initialValue = "",
        isErrorCheck = { it.isEmpty() || it.toBigDecimalOrNull() == null },
        onValueChange = { setValue(it.trim()) },
    )

    val isLong = manager.switchState(false)

    val entry = manager.textFieldState(
        initialValue = "",
        isErrorCheck = { it.isEmpty() || it.toBigDecimalOrNull() == null },
        onValueChange = { setValue(it.trim()) },
    )

    val stop = manager.textFieldState(
        initialValue = "",
        isErrorCheck = { it.isEmpty() || it.toBigDecimalOrNull() == null },
        onValueChange = { setValue(it.trim()) },
    )

    val target = manager.textFieldState(
        initialValue = "",
        isErrorCheck = { it.isEmpty() || it.toBigDecimalOrNull() == null },
        onValueChange = { setValue(it.trim()) },
    )

    fun isValid() = manager.isFormValid()
}
