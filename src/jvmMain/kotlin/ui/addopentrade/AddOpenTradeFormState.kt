package ui.addopentrade

import kotlinx.datetime.*
import ui.common.form.*

internal class AddOpenTradeFormState(
    private val initialModel: Model,
) {

    private val manager = FormManager()

    val ticker = manager.singleSelectionState(initialModel.ticker)

    val quantity = manager.textFieldState(
        initial = initialModel.quantity,
        isErrorCheck = { it.isEmpty() || it.toBigDecimalOrNull() == null },
        onValueChange = { setValue(it.trim()) },
    )

    val isLong = manager.switchState(initialModel.isLong)

    val entry = manager.textFieldState(
        initial = initialModel.entry,
        isErrorCheck = { it.isEmpty() || it.toBigDecimalOrNull() == null },
        onValueChange = { setValue(it.trim()) },
    )

    val stop = manager.textFieldState(
        initial = initialModel.stop,
        isErrorCheck = { it.isEmpty() || it.toBigDecimalOrNull() == null },
        onValueChange = { setValue(it.trim()) },
    )

    val entryDate = manager.dateFieldState(initialModel.entryDateTime.date)

    val entryTime = manager.timeFieldState(initialModel.entryDateTime.time)

    val target = manager.textFieldState(
        initial = initialModel.target,
        isErrorCheck = { it.isEmpty() || it.toBigDecimalOrNull() == null },
        onValueChange = { setValue(it.trim()) },
    )

    private val entryDateTime
        get() = entryDate.value.atTime(entryTime.value)

    fun getModelIfValidOrNull(): Model? = if (!manager.isFormValid()) null else Model(
        id = initialModel.id,
        ticker = ticker.value!!,
        quantity = quantity.value,
        isLong = isLong.value,
        entry = entry.value,
        stop = stop.value,
        entryDateTime = entryDateTime,
        target = target.value,
    )

    class Model(
        val id: Int? = null,
        val ticker: String = "",
        val quantity: String = "",
        val isLong: Boolean = true,
        val entry: String = "",
        val stop: String = "",
        val entryDateTime: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
        val target: String = "",
    )
}
