package ui.opentrades

import androidx.compose.runtime.Immutable
import kotlinx.datetime.*
import ui.common.form.*

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

internal class AddOpenTradeFormState(model: Model?) {

    private val manager = FormManager()

    private val currentDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

    val date = manager.dateFieldState(model?.entryDateTime?.date ?: currentDateTime.date)

    val ticker = manager.singleSelectionState(model?.ticker)

    val quantity = manager.textFieldState(
        initial = model?.quantity ?: "",
        isErrorCheck = { it.isEmpty() || it.toBigDecimalOrNull() == null },
        onValueChange = { setValue(it.trim()) },
    )

    val isLong = manager.switchState(model?.isLong ?: true)

    val entry = manager.textFieldState(
        initial = model?.entry ?: "",
        isErrorCheck = { it.isEmpty() || it.toBigDecimalOrNull() == null },
        onValueChange = { setValue(it.trim()) },
    )

    val stop = manager.textFieldState(
        initial = model?.stop ?: "",
        isErrorCheck = { it.isEmpty() || it.toBigDecimalOrNull() == null },
        onValueChange = { setValue(it.trim()) },
    )

    val entryTime = manager.timeFieldState(model?.entryDateTime?.time ?: currentDateTime.time)

    val target = manager.textFieldState(
        initial = model?.target ?: "",
        isErrorCheck = { it.isEmpty() || it.toBigDecimalOrNull() == null },
        onValueChange = { setValue(it.trim()) },
    )

    val entryDateTime
        get() = date.value.atTime(entryTime.value)

    fun isValid() = manager.isFormValid()

    class Model(
        val id: Int?,
        val ticker: String,
        val quantity: String,
        val isLong: Boolean,
        val entry: String,
        val stop: String,
        val entryDateTime: LocalDateTime,
        val target: String,
    )
}
