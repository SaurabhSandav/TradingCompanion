package com.saurabhsandav.core.ui.tradeorders

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trades.TradeOrder
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.ui.common.CollectEffect
import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.tradeorderform.model.OrderFormType
import com.saurabhsandav.core.ui.tradeorders.model.TradeOrdersEvent
import com.saurabhsandav.core.ui.tradeorders.model.TradeOrdersEvent.*
import com.saurabhsandav.core.ui.tradeorders.model.TradeOrdersState
import com.saurabhsandav.core.ui.tradeorders.model.TradeOrdersState.*
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
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
    private val tradingProfiles: TradingProfiles = appModule.tradingProfiles,
) {

    private val events = MutableSharedFlow<TradeOrdersEvent>(extraBufferCapacity = Int.MAX_VALUE)

    private var orderFormParams by mutableStateOf(persistentListOf<OrderFormParams>())

    val state = coroutineScope.launchMolecule(RecompositionClock.ContextClock) {

        CollectEffect(events) { event ->

            when (event) {
                NewOrder -> onNewOrder()
                is NewOrderFromExisting -> onNewOrderFromExisting(event.profileOrderId)
                is EditOrder -> onEditOrder(event.profileOrderId)
                is CloseOrderForm -> onCloseOrderForm(event.id)
                is LockOrder -> onLockOrder(event.profileOrderId)
                is DeleteOrder -> onDeleteOrder(event.profileOrderId)
            }
        }

        return@launchMolecule TradeOrdersState(
            tradeOrderItems = getTradeListEntries().value,
            orderFormParams = orderFormParams,
        )
    }

    val errors = mutableStateListOf<UIErrorMessage>()

    fun event(event: TradeOrdersEvent) {
        events.tryEmit(event)
    }

    @Composable
    private fun getTradeListEntries(): State<ImmutableList<TradeOrderListItem>> {
        return remember {
            tradingProfiles.currentProfile.flatMapLatest { profile ->

                // Close all child windows
                orderFormParams = orderFormParams.clear()

                val tradingRecord = tradingProfiles.getRecord(profile.id)

                tradingRecord.orders.allOrders.map { orders ->
                    orders.groupBy { it.timestamp.date }
                        .map { (date, list) ->
                            listOf(
                                date.toTradeOrderListDayHeader(),
                                TradeOrderListItem.Entries(list.map { it.toTradeOrderListEntry(profile.id) }
                                    .toImmutableList()),
                            )
                        }.flatten().toImmutableList()
                }
            }
        }.collectAsState(persistentListOf())
    }

    private fun LocalDate.toTradeOrderListDayHeader(): TradeOrderListItem.DayHeader {
        val formatted = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(toJavaLocalDate())
        return TradeOrderListItem.DayHeader(formatted)
    }

    private fun TradeOrder.toTradeOrderListEntry(profileId: Long) = TradeOrderEntry(
        profileOrderId = ProfileOrderId(profileId = profileId, orderId = id),
        broker = run {
            val instrumentCapitalized = instrument.strValue
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            "$broker ($instrumentCapitalized)"
        },
        ticker = ticker,
        quantity = lots?.let { "$quantity ($it ${if (it == 1) "lot" else "lots"})" } ?: quantity.toString(),
        type = type.strValue.uppercase(),
        price = price.toPlainString(),
        timestamp = timestamp.time.toString(),
        locked = locked,
    )

    private fun onNewOrder() = coroutineScope.launchUnit {

        val currentProfile = tradingProfiles.currentProfile.first()

        val params = OrderFormParams(
            id = UUID.randomUUID(),
            profileId = currentProfile.id,
            formType = OrderFormType.New(),
        )

        orderFormParams = orderFormParams.add(params)
    }

    private fun onNewOrderFromExisting(profileOrderId: ProfileOrderId) {

        // Don't allow opening duplicate windows
        val isWindowAlreadyOpen = orderFormParams.any {
            it.formType is OrderFormType.NewFromExisting && it.formType.id == profileOrderId.orderId
        }
        if (isWindowAlreadyOpen) return

        val params = OrderFormParams(
            id = UUID.randomUUID(),
            profileId = profileOrderId.profileId,
            formType = OrderFormType.NewFromExisting(profileOrderId.orderId),
        )

        orderFormParams = orderFormParams.add(params)
    }

    private fun onEditOrder(profileOrderId: ProfileOrderId) {

        // Don't allow opening duplicate windows
        val isWindowAlreadyOpen = orderFormParams.any {
            it.formType is OrderFormType.Edit && it.formType.id == profileOrderId.orderId
        }
        if (isWindowAlreadyOpen) return

        val params = OrderFormParams(
            id = UUID.randomUUID(),
            profileId = profileOrderId.profileId,
            formType = OrderFormType.Edit(profileOrderId.orderId),
        )

        orderFormParams = orderFormParams.add(params)
    }

    private fun onCloseOrderForm(id: UUID) {

        val params = orderFormParams.first { it.id == id }

        orderFormParams = orderFormParams.remove(params)
    }

    private fun onLockOrder(profileOrderId: ProfileOrderId) = coroutineScope.launchUnit {

        val tradingRecord = tradingProfiles.getRecord(profileOrderId.profileId)

        tradingRecord.orders.lockOrder(profileOrderId.orderId)
    }

    private fun onDeleteOrder(profileOrderId: ProfileOrderId) = coroutineScope.launchUnit {

        val tradingRecord = tradingProfiles.getRecord(profileOrderId.profileId)

        tradingRecord.orders.delete(profileOrderId.orderId)
    }
}
