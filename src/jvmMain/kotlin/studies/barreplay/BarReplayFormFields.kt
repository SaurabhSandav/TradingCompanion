package studies.barreplay

import kotlinx.datetime.LocalDateTime
import ui.common.form.FormScope
import ui.common.form.dateTimeFieldState
import ui.common.form.singleSelectionState

internal class BarReplayFormFields(
    formScope: FormScope,
    initial: Model,
) {

    // TODO remove initial
    val symbol = formScope.singleSelectionState("ICICIBANK")

    // TODO remove initial
    val timeframe = formScope.singleSelectionState("5m")

    val dataFrom = formScope.dateTimeFieldState(initial.dataFrom)

    val dataTo = formScope.dateTimeFieldState(initial.dataTo)

    val replayFrom = formScope.dateTimeFieldState(initial.replayFrom)

    class Model(
        val symbol: String?,
        val timeframe: String?,
        val dataFrom: LocalDateTime,
        val dataTo: LocalDateTime,
        val replayFrom: LocalDateTime,
    )
}
