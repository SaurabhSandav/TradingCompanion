package ui.closedtrades.model

import androidx.compose.runtime.mutableStateListOf
import ui.addclosedtradedetailed.CloseTradeDetailedFormFields

internal class EditClosedTradeWindowsManager {

    val windows = mutableStateListOf<EditWindowState>()

    fun openNewWindow(formModel: CloseTradeDetailedFormFields.Model) {

        windows += EditWindowState(
            formModel = formModel,
            onCloseRequest = windows::remove,
        )
    }
}

internal class EditWindowState(
    val formModel: CloseTradeDetailedFormFields.Model,
    val onCloseRequest: (EditWindowState) -> Unit,
) {

    fun close() = onCloseRequest(this)
}
