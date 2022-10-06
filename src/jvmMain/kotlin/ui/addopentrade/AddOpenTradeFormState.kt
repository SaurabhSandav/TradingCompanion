package ui.addopentrade

import kotlinx.datetime.*
import ui.common.form.*

internal class AddOpenTradeFormState(
    private val formScope: FormScope,
    private val initialModel: Model,
) {

    val ticker = formScope.singleSelectionState(initialModel.ticker)

    val quantity = formScope.textFieldState(
        initial = initialModel.quantity,
        isErrorCheck = { it.isEmpty() || it.toBigDecimalOrNull() == null },
        onValueChange = { setValue(it.trim()) },
    )

    val isLong = formScope.switchState(initialModel.isLong)

    val entry = formScope.textFieldState(
        initial = initialModel.entry,
        isErrorCheck = { it.isEmpty() || it.toBigDecimalOrNull() == null },
        onValueChange = { setValue(it.trim()) },
    )

    val stop = formScope.textFieldState(
        initial = initialModel.stop,
        isErrorCheck = { it.isEmpty() || it.toBigDecimalOrNull() == null },
        onValueChange = { setValue(it.trim()) },
    )

    val entryDate = formScope.dateFieldState(initialModel.entryDateTime.date)

    val entryTime = formScope.timeFieldState(initialModel.entryDateTime.time)

    val target = formScope.textFieldState(
        initial = initialModel.target,
        isErrorCheck = { it.isEmpty() || it.toBigDecimalOrNull() == null },
        onValueChange = { setValue(it.trim()) },
    )

    private val entryDateTime
        get() = entryDate.value.atTime(entryTime.value)

    fun getModelIfValidOrNull(): Model? = if (!formScope.isFormValid()) null else Model(
        id = initialModel.id,
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
