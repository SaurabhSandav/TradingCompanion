package ui.tradeorders.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class TradeOrdersState(
    val tradeOrderItems: Map<TradeOrderListItem.DayHeader, List<TradeOrderListItem.Entry>>,
    val deleteConfirmationDialogState: DeleteConfirmationDialog,
) {

    @Immutable
    internal sealed class DeleteConfirmationDialog {

        @Immutable
        data class Open(val id: Long) : DeleteConfirmationDialog()

        @Immutable
        object Dismissed : DeleteConfirmationDialog()
    }
}

@Immutable
internal sealed class TradeOrderListItem {

    @Immutable
    internal data class DayHeader(val header: String) : TradeOrderListItem()

    @Immutable
    internal data class Entry(
        val id: Long,
        val broker: String,
        val ticker: String,
        val quantity: String,
        val type: String,
        val price: String,
        val timestamp: String,
    ) : TradeOrderListItem()
}
