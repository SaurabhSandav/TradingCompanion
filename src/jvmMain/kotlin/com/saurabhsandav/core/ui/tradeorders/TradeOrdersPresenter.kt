package com.saurabhsandav.core.ui.tradeorders

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trades.TradeOrdersRepo
import com.saurabhsandav.core.trades.model.TradeOrder
import com.saurabhsandav.core.ui.common.CollectEffect
import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.tradeorders.model.TradeOrderListItem
import com.saurabhsandav.core.ui.tradeorders.model.TradeOrdersEvent
import com.saurabhsandav.core.ui.tradeorders.model.TradeOrdersEvent.*
import com.saurabhsandav.core.ui.tradeorders.model.TradeOrdersState
import com.saurabhsandav.core.ui.tradeorders.orderform.OrderFormWindowParams
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import com.saurabhsandav.core.ui.tradeorders.model.TradeOrdersEvent.DeleteConfirmationDialog as DeleteConfirmationDialogEvent
import com.saurabhsandav.core.ui.tradeorders.model.TradeOrdersState.DeleteConfirmationDialog as DeleteConfirmationDialogState

internal class TradeOrdersPresenter(
    private val coroutineScope: CoroutineScope,
    private val appModule: AppModule,
    private val tradeOrdersRepo: TradeOrdersRepo = appModule.tradeOrdersRepo,
) {

    private val events = MutableSharedFlow<TradeOrdersEvent>(extraBufferCapacity = Int.MAX_VALUE)

    private val orderFormWindowParams = mutableStateMapOf<UUID, OrderFormWindowParams>()

    val state = coroutineScope.launchMolecule(RecompositionClock.ContextClock) {

        CollectEffect(events) { event ->

            when (event) {
                NewOrder -> onNewOrder()
                is NewOrderFromExisting -> onNewOrderFromExisting(event.id)
                is EditOrder -> onEditOrder(event.id)
                else -> Unit
            }
        }

        return@launchMolecule TradeOrdersState(
            tradeOrderItems = getTradeListEntries().value,
            orderFormWindowParams = orderFormWindowParams.values,
            deleteConfirmationDialogState = deleteConfirmationDialogState(events),
        )
    }

    val errors = mutableStateListOf<UIErrorMessage>()

    fun event(event: TradeOrdersEvent) {
        events.tryEmit(event)
    }

    @Composable
    private fun getTradeListEntries(): State<Map<TradeOrderListItem.DayHeader, List<TradeOrderListItem.Entry>>> {
        return remember {
            tradeOrdersRepo.allOrders.map { orders ->
                orders.groupBy { it.timestamp.date }
                    .mapKeys { (date, _) -> date.toTradeOrderListDayHeader() }
                    .mapValues { (_, list) -> list.map { it.toTradeOrderListEntry() } }
            }
        }.collectAsState(emptyMap())
    }

    private fun LocalDate.toTradeOrderListDayHeader(): TradeOrderListItem.DayHeader {
        val formatted = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(toJavaLocalDate())
        return TradeOrderListItem.DayHeader(formatted)
    }

    private fun TradeOrder.toTradeOrderListEntry() = TradeOrderListItem.Entry(
        id = id,
        broker = broker,
        ticker = ticker,
        quantity = lots?.let { "$quantity ($it ${if (it == 1) "lot" else "lots"})" } ?: quantity.toString(),
        type = type.strValue.uppercase(),
        price = price.toPlainString(),
        timestamp = timestamp.time.toString(),
    )

    private fun onNewOrder() {

        val key = UUID.randomUUID()
        val params = OrderFormWindowParams(
            operationType = OrderFormWindowParams.OperationType.New,
            onCloseRequest = { orderFormWindowParams.remove(key) }
        )

        orderFormWindowParams[key] = params
    }

    private fun onNewOrderFromExisting(id: Long) {

        // Don't allow opening duplicate windows
        val isWindowAlreadyOpen = orderFormWindowParams.values.any {
            it.operationType is OrderFormWindowParams.OperationType.NewFromExisting && it.operationType.id == id
        }
        if (isWindowAlreadyOpen) return

        val key = UUID.randomUUID()
        val params = OrderFormWindowParams(
            operationType = OrderFormWindowParams.OperationType.NewFromExisting(id),
            onCloseRequest = { orderFormWindowParams.remove(key) }
        )

        orderFormWindowParams[key] = params
    }

    private fun onEditOrder(id: Long) {

        // Don't allow opening duplicate windows
        val isWindowAlreadyOpen = orderFormWindowParams.values.any {
            it.operationType is OrderFormWindowParams.OperationType.EditExisting && it.operationType.id == id
        }
        if (isWindowAlreadyOpen) return

        val key = UUID.randomUUID()
        val params = OrderFormWindowParams(
            operationType = OrderFormWindowParams.OperationType.EditExisting(id),
            onCloseRequest = { orderFormWindowParams.remove(key) }
        )

        orderFormWindowParams[key] = params
    }

    @Composable
    private fun deleteConfirmationDialogState(events: Flow<TradeOrdersEvent>): DeleteConfirmationDialogState {

        var state by state<DeleteConfirmationDialogState> { DeleteConfirmationDialogState.Dismissed }

        CollectEffect(events) { event ->

            state = when (event) {
                is DeleteOrder -> DeleteConfirmationDialogState.Open(event.id)

                is DeleteConfirmationDialogEvent.Confirm -> {
                    deleteOrder(event.id)
                    DeleteConfirmationDialogState.Dismissed
                }

                DeleteConfirmationDialogEvent.Dismiss -> DeleteConfirmationDialogState.Dismissed
                else -> state
            }
        }

        return state
    }

    private fun deleteOrder(id: Long) = coroutineScope.launchUnit {
        tradeOrdersRepo.delete(id)
    }
}
