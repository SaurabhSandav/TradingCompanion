package ui.barreplay.launchform

import kotlinx.datetime.LocalDateTime
import ui.common.form.FormScope
import ui.common.form.dateTimeFieldState
import ui.common.form.singleSelectionState
import ui.common.form.switchState

internal class ReplayLaunchFormFields(
    private val formScope: FormScope,
    initial: Model,
) {

    val baseTimeframe = formScope.singleSelectionState(initial.baseTimeframe)

    val dataFrom = formScope.dateTimeFieldState(initial.dataFrom)

    val dataTo = formScope.dateTimeFieldState(initial.dataTo)

    val replayFrom = formScope.dateTimeFieldState(initial.replayFrom)

    val replayFullBar = formScope.switchState(initial.replayFullBar)

    val initialSymbol = formScope.singleSelectionState(initial.initialSymbol)

    fun getModelIfValidOrNull(): Model? = if (!formScope.isFormValid()) null else Model(
        baseTimeframe = baseTimeframe.value,
        dataFrom = dataFrom.value,
        dataTo = dataTo.value,
        replayFrom = replayFrom.value,
        replayFullBar = replayFullBar.value,
        initialSymbol = initialSymbol.value,
    )

    class Model(
        val baseTimeframe: String?,
        val dataFrom: LocalDateTime,
        val dataTo: LocalDateTime,
        val replayFrom: LocalDateTime,
        val replayFullBar: Boolean,
        val initialSymbol: String?,
    )
}
