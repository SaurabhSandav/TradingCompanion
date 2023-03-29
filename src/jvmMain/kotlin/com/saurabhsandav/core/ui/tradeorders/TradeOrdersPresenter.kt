package com.saurabhsandav.core.ui.tradeorders

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trades.TradeOrder
import com.saurabhsandav.core.trades.TradeOrdersRepo
import com.saurabhsandav.core.ui.common.CollectEffect
import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.tradeorderform.OrderFormWindowParams
import com.saurabhsandav.core.ui.tradeorders.model.TradeOrdersEvent
import com.saurabhsandav.core.ui.tradeorders.model.TradeOrdersEvent.*
import com.saurabhsandav.core.ui.tradeorders.model.TradeOrdersState
import com.saurabhsandav.core.ui.tradeorders.model.TradeOrdersState.TradeOrderEntry
import com.saurabhsandav.core.ui.tradeorders.model.TradeOrdersState.TradeOrderListItem
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

@Stable
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
                is LockOrder -> onLockOrder(event.id)
                is EditOrder -> onEditOrder(event.id)
                is DeleteOrder -> onDeleteOrder(event.id)
            }
        }

        return@launchMolecule TradeOrdersState(
            tradeOrderItems = getTradeListEntries().value,
            orderFormWindowParams = orderFormWindowParams.values,
        )
    }

    val errors = mutableStateListOf<UIErrorMessage>()

    fun event(event: TradeOrdersEvent) {
        events.tryEmit(event)
    }

    @Composable
    private fun getTradeListEntries(): State<ImmutableList<TradeOrderListItem>> {
        return remember {
            tradeOrdersRepo.allOrders.map { orders ->
                orders.groupBy { it.timestamp.date }
                    .map { (date, list) ->
                        listOf(
                            date.toTradeOrderListDayHeader(),
                            TradeOrderListItem.Entries(list.map { it.toTradeOrderListEntry() }.toImmutableList()),
                        )
                    }.flatten().toImmutableList()
            }
        }.collectAsState(persistentListOf())
    }

    private fun LocalDate.toTradeOrderListDayHeader(): TradeOrderListItem.DayHeader {
        val formatted = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(toJavaLocalDate())
        return TradeOrderListItem.DayHeader(formatted)
    }

    private fun TradeOrder.toTradeOrderListEntry() = TradeOrderEntry(
        id = id,
        broker = broker,
        ticker = ticker,
        quantity = lots?.let { "$quantity ($it ${if (it == 1) "lot" else "lots"})" } ?: quantity.toString(),
        type = type.strValue.uppercase(),
        price = price.toPlainString(),
        timestamp = timestamp.time.toString(),
        locked = locked,
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

    private fun onLockOrder(id: Long) = coroutineScope.launchUnit {
        tradeOrdersRepo.lockOrder(id)
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

    private fun onDeleteOrder(id: Long) = coroutineScope.launchUnit {
        tradeOrdersRepo.delete(id)
    }
}
