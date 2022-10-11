package ui.addopentrade

import kotlinx.datetime.*
import ui.common.form.*

internal class AddOpenTradeFormFields(
    private val formScope: FormScope,
    private val initial: Model,
) {

    val ticker = formScope.singleSelectionState(initial.ticker)

    val quantity = formScope.textFieldState(
        initial = initial.quantity,
        isErrorCheck = { it.isEmpty() || it.toBigDecimalOrNull() == null },
        onValueChange = { setValue(it.trim()) },
    )

    val isLong = formScope.switchState(initial.isLong)

    val entry = formScope.textFieldState(
        initial = initial.entry,
        isErrorCheck = { it.isEmpty() || it.toBigDecimalOrNull() == null },
        onValueChange = { setValue(it.trim()) },
    )

    val stop = formScope.textFieldState(
        initial = initial.stop,
        isErrorCheck = { it.isEmpty() || it.toBigDecimalOrNull() == null },
        onValueChange = { setValue(it.trim()) },
    )

    val entryDate = formScope.dateFieldState(initial.entryDateTime.date)

    val entryTime = formScope.timeFieldState(initial.entryDateTime.time)

    private val entryDateTime
        get() = entryDate.value.atTime(entryTime.value)

    val target = formScope.textFieldState(
        initial = initial.target,
        isErrorCheck = { it.isEmpty() || it.toBigDecimalOrNull() == null },
        onValueChange = { setValue(it.trim()) },
    )

    fun getModelIfValidOrNull(): Model? = if (!formScope.isFormValid()) null else Model(
        id = initial.id,
        ticker = ticker.value,
        quantity = quantity.value,
        isLong = isLong.value,
        entry = entry.value,
        stop = stop.value,
        entryDateTime = entryDateTime,
        target = target.value,
    )

    class Model(
        val id: Int? = null,
        val ticker: String? = null,
        val quantity: String = "",
        val isLong: Boolean = true,
        val entry: String = "",
        val stop: String = "",
        val entryDateTime: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
        val target: String = "",
    )
}
