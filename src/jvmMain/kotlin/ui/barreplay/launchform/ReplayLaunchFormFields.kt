package ui.barreplay.launchform

import kotlinx.datetime.LocalDateTime
import ui.common.form.*

internal class ReplayLaunchFormFields(
    private val formScope: FormScope,
    initial: Model,
) {

    val baseTimeframe = formScope.singleSelectionState(initial.baseTimeframe)

    val candlesBefore = formScope.textFieldState(
        initial = initial.candlesBefore.toString(),
        isErrorCheck = { it.isEmpty() || it.toIntOrNull() == null },
        onValueChange = { setValue(it.trim()) },
    )

    val dataTo = formScope.dateTimeFieldState(initial.dataTo)

    val replayFrom = formScope.dateTimeFieldState(initial.replayFrom)

    val replayFullBar = formScope.switchState(initial.replayFullBar)

    val initialSymbol = formScope.singleSelectionState(initial.initialSymbol)

    fun getModelIfValidOrNull(): Model? = if (!formScope.isFormValid()) null else Model(
        baseTimeframe = baseTimeframe.value,
        candlesBefore = candlesBefore.value.toInt(),
        replayFrom = replayFrom.value,
        dataTo = dataTo.value,
        replayFullBar = replayFullBar.value,
        initialSymbol = initialSymbol.value,
    )

    class Model(
        val baseTimeframe: String?,
        val candlesBefore: Int,
        val replayFrom: LocalDateTime,
        val dataTo: LocalDateTime,
        val replayFullBar: Boolean,
        val initialSymbol: String?,
    )
}
