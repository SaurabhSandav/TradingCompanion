package ui.closedtrades.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.snapshots.SnapshotStateList
import ui.addclosedtradedetailed.CloseTradeDetailedWindowState
import ui.common.MultipleWindowManager
import ui.fyerslogin.FyersLoginState

@Immutable
internal data class ClosedTradesState(
    val closedTradesItems: Map<ClosedTradeListItem.DayHeader, List<ClosedTradeListItem.Entry>>,
    val deleteConfirmationDialogState: DeleteConfirmationDialog,
    val editTradeWindowStates: SnapshotStateList<CloseTradeDetailedWindowState>,
    val chartWindowsManager: MultipleWindowManager<ClosedTradeChartWindowParams>,
    val fyersLoginWindowState: FyersLoginWindow,
) {

    @Immutable
    internal sealed class DeleteConfirmationDialog {

        @Immutable
        data class Open(val id: Long) : DeleteConfirmationDialog()

        @Immutable
        object Dismissed : DeleteConfirmationDialog()
    }

    @Immutable
    internal sealed class FyersLoginWindow {

        @Immutable
        class Open(val fyersLoginState: FyersLoginState) : FyersLoginWindow()

        @Immutable
        object Closed : FyersLoginWindow()
    }
}

@Immutable
internal sealed class ClosedTradeListItem {

    @Immutable
    internal data class DayHeader(val header: String) : ClosedTradeListItem()

    @Immutable
    internal data class Entry(
        val id: Long,
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
