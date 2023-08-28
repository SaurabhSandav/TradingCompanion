package com.saurabhsandav.core.ui.tradeorders

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trades.TradeOrder
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.common.app.AppWindowsManager
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

    private val orderFormWindowsManager = AppWindowsManager<OrderFormParams>()
    private val errors = mutableStateListOf<UIErrorMessage>()

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule TradeOrdersState(
            tradeOrderItems = getTradeListEntries().value,
            orderFormWindowsManager = orderFormWindowsManager,
            errors = remember(errors) { errors.toImmutableList() },
            eventSink = ::onEvent,
        )
    }

    private fun onEvent(event: TradeOrdersEvent) {

        when (event) {
            NewOrder -> onNewOrder()
            is NewOrderFromExisting -> onNewOrderFromExisting(event.profileOrderId)
            is EditOrder -> onEditOrder(event.profileOrderId)
            is LockOrders -> onLockOrders(event.ids)
            is DeleteOrders -> onDeleteOrders(event.ids)
        }
    }

    @Composable
    private fun getTradeListEntries(): State<ImmutableList<TradeOrderListItem>> {
        return remember {
            tradingProfiles.currentProfile.flatMapLatest { profile ->

                // Close all child windows
                orderFormWindowsManager.closeAll()

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

        orderFormWindowsManager.newWindow(params)
    }

    private fun onNewOrderFromExisting(profileOrderId: ProfileOrderId) {

        val window = orderFormWindowsManager.windows.find {
            it.params.formType is OrderFormType.NewFromExisting && it.params.formType.id == profileOrderId.orderId
        }

        when (window) {
            // Open new window
            null -> {

                val params = OrderFormParams(
                    id = UUID.randomUUID(),
                    profileId = profileOrderId.profileId,
                    formType = OrderFormType.NewFromExisting(profileOrderId.orderId),
                )

                orderFormWindowsManager.newWindow(params)
            }

            // Window already open. Bring to front.
            else -> window.toFront()
        }
    }

    private fun onEditOrder(profileOrderId: ProfileOrderId) {

        val window = orderFormWindowsManager.windows.find {
            it.params.formType is OrderFormType.Edit && it.params.formType.id == profileOrderId.orderId
        }

        when (window) {
            // Open new window
            null -> {

                val params = OrderFormParams(
                    id = UUID.randomUUID(),
                    profileId = profileOrderId.profileId,
                    formType = OrderFormType.Edit(profileOrderId.orderId),
                )

                orderFormWindowsManager.newWindow(params)
            }

            // Window already open. Bring to front.
            else -> window.toFront()
        }
    }

    private fun onLockOrders(ids: List<ProfileOrderId>) = coroutineScope.launchUnit {

        ids.groupBy { it.profileId }.forEach { (profileId, ids) ->

            val tradingRecord = tradingProfiles.getRecord(profileId)

            tradingRecord.orders.lock(ids.map { it.orderId })
        }
    }

    private fun onDeleteOrders(ids: List<ProfileOrderId>) = coroutineScope.launchUnit {

        ids.groupBy { it.profileId }.forEach { (profileId, ids) ->

            val tradingRecord = tradingProfiles.getRecord(profileId)

            tradingRecord.orders.delete(ids.map { it.orderId })
        }
    }
}
