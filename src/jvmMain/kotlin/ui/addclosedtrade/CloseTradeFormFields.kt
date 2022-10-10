package ui.addclosedtrade

import kotlinx.datetime.*
import ui.addopentrade.AddOpenTradeFormFields
import ui.common.form.FormScope
import ui.common.form.dateFieldState
import ui.common.form.textFieldState
import ui.common.form.timeFieldState

internal class CloseTradeFormFields(
    private val formScope: FormScope,
    private val openTradeModel: AddOpenTradeFormFields.Model,
) {

    private val currentDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

    val exit = formScope.textFieldState(
        initial = "",
        isErrorCheck = { it.isEmpty() || it.toBigDecimalOrNull() == null },
        onValueChange = { setValue(it.trim()) },
    )

    val exitDate = formScope.dateFieldState(currentDateTime.date)

    val exitTime = formScope.timeFieldState(currentDateTime.time)

    private val exitDateTime
        get() = exitDate.value.atTime(exitTime.value)

    fun getModelIfValidOrNull(): Model? = if (!formScope.isFormValid()) null else Model(
        openTradeModel = openTradeModel,
        exit = exit.value,
        exitDateTime = exitDateTime,
    )

    class Model(
        val openTradeModel: AddOpenTradeFormFields.Model,
        val exit: String,
        val exitDateTime: LocalDateTime,
    )
}
