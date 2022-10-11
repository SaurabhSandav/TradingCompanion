package ui.closedtrades.model

import androidx.compose.runtime.Immutable
import ui.addclosedtradedetailed.CloseTradeDetailedFormFields

@Immutable
internal data class ClosedTradesState(
    val closedTradesItems: Map<ClosedTradeListItem.DayHeader, List<ClosedTradeListItem.Entry>>,
    val deleteConfirmationDialogState: DeleteConfirmationDialog,
    val closeTradeDetailedWindowState: EditTradeWindow,
) {

    @Immutable
    internal sealed class DeleteConfirmationDialog {

        @Immutable
        data class Open(val id: Int) : DeleteConfirmationDialog()

        @Immutable
        object Dismissed : DeleteConfirmationDialog()
    }

    @Immutable
    internal sealed class EditTradeWindow {

        @Immutable
        data class Open(val formModel: CloseTradeDetailedFormFields.Model) : EditTradeWindow()

        @Immutable
        object Closed : EditTradeWindow()
    }
}

@Immutable
internal sealed class ClosedTradeListItem {

    @Immutable
    internal data class DayHeader(val header: String) : ClosedTradeListItem()

    @Immutable
    internal data class Entry(
        val id: Int,
        val broker: String,
        val ticker: String,
        val quantity: String,
        val side: String,
        val entry: String,
        val stop: String,
        val duration: String,
        val target: String,
        val exit: String,
        val pnl: String,
        val isProfitable: Boolean,
        val netPnl: String,
        val isNetProfitable: Boolean,
        val fees: String,
        val maxFavorableExcursion: String,
        val maxAdverseExcursion: String,
        val persisted: Boolean,
        val persistenceResult: String?,
    ) : ClosedTradeListItem()
}
