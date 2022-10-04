package ui.opentrades

import androidx.compose.runtime.Immutable
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import ui.common.form.FormManager
import ui.common.form.dateFieldState
import ui.common.form.timeFieldState

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

    private val currentDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

    val date = manager.dateFieldState(
        initial = currentDateTime.date,
    )

    val ticker = manager.singleSelectionState(
        labelText = "Select Stock...",
    )

    val quantity = manager.textFieldState(
        initial = "",
        isErrorCheck = { it.isEmpty() || it.toBigDecimalOrNull() == null },
        onValueChange = { setValue(it.trim()) },
    )

    val isLong = manager.switchState(false)

    val entry = manager.textFieldState(
        initial = "",
        isErrorCheck = { it.isEmpty() || it.toBigDecimalOrNull() == null },
        onValueChange = { setValue(it.trim()) },
    )

    val stop = manager.textFieldState(
        initial = "",
        isErrorCheck = { it.isEmpty() || it.toBigDecimalOrNull() == null },
        onValueChange = { setValue(it.trim()) },
    )

    val entryTime = manager.timeFieldState(
        initial = currentDateTime.time,
    )

    val target = manager.textFieldState(
        initial = "",
        isErrorCheck = { it.isEmpty() || it.toBigDecimalOrNull() == null },
        onValueChange = { setValue(it.trim()) },
    )

    fun isValid() = manager.isFormValid()
}
