package ui.closedtrades.model

import androidx.compose.runtime.mutableStateListOf
import ui.addclosedtradedetailed.CloseTradeDetailedFormFields
import utils.CandleRepo

internal class ClosedTradeChartWindowsManager(val candleRepo: CandleRepo) {

    val windows = mutableStateListOf<ChartWindowState>()

    fun openNewWindow(formModel: CloseTradeDetailedFormFields.Model) {

        windows += ChartWindowState(
            formModel = formModel,
            onCloseRequest = windows::remove,
        )
    }
}

internal class ChartWindowState(
    val formModel: CloseTradeDetailedFormFields.Model,
    val onCloseRequest: (ChartWindowState) -> Unit,
) {

    fun close() = onCloseRequest(this)
}
