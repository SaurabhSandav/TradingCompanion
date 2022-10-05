package ui.addclosedtrade

import kotlinx.datetime.*
import ui.addopentrade.AddOpenTradeFormState
import ui.common.form.FormManager
import ui.common.form.dateFieldState
import ui.common.form.textFieldState
import ui.common.form.timeFieldState

internal class CloseTradeFormState {

    private val manager = FormManager()

    private val currentDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

    val exit = manager.textFieldState(
        initial = "",
        isErrorCheck = { it.isEmpty() || it.toBigDecimalOrNull() == null },
        onValueChange = { setValue(it.trim()) },
    )

    val exitDate = manager.dateFieldState(currentDateTime.date)

    val exitTime = manager.timeFieldState(currentDateTime.time)

    val exitDateTime
        get() = exitDate.value.atTime(exitTime.value)

    fun isValid() = manager.isFormValid()

    class Model(
        val openTradeModel: AddOpenTradeFormState.Model,
        val exit: String,
        val exitDateTime: LocalDateTime,
    )
}
