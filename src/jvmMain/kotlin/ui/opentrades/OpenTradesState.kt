package ui.opentrades

import androidx.compose.runtime.Immutable
import kotlinx.datetime.*
import ui.common.form.FormManager
import ui.common.form.dateFieldState
import ui.common.form.timeFieldState

@Immutable
internal data class OpenTradesState(
    val openTrades: List<OpenTradeListEntry>,
    val addTradeWindowState: AddTradeWindowState,
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

internal class AddOpenTradeFormState(addOpenTradeFormStateModel: Model?) {

    private val manager = FormManager()

    private val currentDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

    val date = manager.dateFieldState(
        initial = currentDateTime.date,
    )

    val ticker = manager.singleSelectionState()

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

    val entryDateTime = date.value.atTime(entryTime.value)

    fun isValid() = manager.isFormValid()

    class Model(
        val ticker: String,
        val quantity: String,
        val isLong: Boolean,
        val entry: String,
        val stop: String,
        val entryDateTime: LocalDateTime,
        val target: String,
    )
}
