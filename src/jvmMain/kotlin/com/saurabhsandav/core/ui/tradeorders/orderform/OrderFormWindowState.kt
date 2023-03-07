package com.saurabhsandav.core.ui.tradeorders.orderform

import androidx.compose.runtime.*
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.LocalAppModule
import com.saurabhsandav.core.trades.TradeOrdersRepo
import com.saurabhsandav.core.trades.model.OrderType
import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.ui.tradeorders.orderform.OrderFormWindowParams.OperationType.*
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.nanoseconds

@Composable
internal fun rememberOrderFormWindowState(
    params: OrderFormWindowParams,
): OrderFormWindowState {

    val scope = rememberCoroutineScope()
    val appModule = LocalAppModule.current

    return remember {
        OrderFormWindowState(
            params = params,
            coroutineScope = scope,
            appModule = appModule,
        )
    }
}

@Stable
internal class OrderFormWindowState(
    val params: OrderFormWindowParams,
    private val coroutineScope: CoroutineScope,
    private val appModule: AppModule,
    private val tradeOrdersRepo: TradeOrdersRepo = appModule.tradeOrdersRepo,
) {

    private val formValidator = FormValidator()

    var isReady by mutableStateOf(false)
        private set

    var windowTitle by mutableStateOf("")

    val model = OrderFormModel(
        validator = formValidator,
        ticker = null,
        quantity = "",
        isBuy = true,
        price = "",
        timestamp = run {
            val currentTime = Clock.System.now()
            val currentTimeWithoutNanoseconds = currentTime - currentTime.nanosecondsOfSecond.nanoseconds
            currentTimeWithoutNanoseconds.toLocalDateTime(TimeZone.currentSystemDefault())
        },
    )

    init {

        coroutineScope.launch {

            when (params.operationType) {
                New -> windowTitle = "New Order"
                is NewFromExisting -> newFromExisting(params.operationType.id)
                is EditExisting -> editExistingOrder(params.operationType.id)
            }

            isReady = true
        }
    }

    fun onSaveOrder() = coroutineScope.launchUnit {

        if (!formValidator.isValid()) return@launchUnit

        when (params.operationType) {
            is EditExisting -> {
                tradeOrdersRepo.edit(
                    id = params.operationType.id,
                    broker = "Finvasia",
                    ticker = model.ticker.value!!,
                    quantity = model.quantity.value.toBigDecimal(),
                    lots = null,
                    type = if (model.isBuy.value) OrderType.Buy else OrderType.Sell,
                    price = model.price.value.toBigDecimal(),
                    timestamp = model.timestamp.value,
                )
            }

            else -> {
                tradeOrdersRepo.new(
                    broker = "Finvasia",
                    ticker = model.ticker.value!!,
                    quantity = model.quantity.value.toBigDecimal(),
                    lots = null,
                    type = if (model.isBuy.value) OrderType.Buy else OrderType.Sell,
                    price = model.price.value.toBigDecimal(),
                    timestamp = model.timestamp.value,
                    locked = false,
                )
            }
        }

        params.onCloseRequest()
    }

    private suspend fun newFromExisting(id: Long) {

        windowTitle = "New Order"

        val order = tradeOrdersRepo.getById(id).first()

        model.ticker.value = order.ticker
        model.quantity.value = order.quantity.toString()
        model.isBuy.value = order.type == OrderType.Buy
        model.price.value = order.price.toPlainString()
        model.timestamp.value = run {
            val currentTime = Clock.System.now()
            val currentTimeWithoutNanoseconds = currentTime - currentTime.nanosecondsOfSecond.nanoseconds
            currentTimeWithoutNanoseconds.toLocalDateTime(TimeZone.currentSystemDefault())
        }
    }

    private suspend fun editExistingOrder(id: Long) {

        windowTitle = "Edit Order"

        val order = tradeOrdersRepo.getById(id).first()

        model.ticker.value = order.ticker
        model.quantity.value = order.quantity.toString()
        model.isBuy.value = order.type == OrderType.Buy
        model.price.value = order.price.toPlainString()
        model.timestamp.value = order.timestamp
    }
}

internal class OrderFormWindowParams(
    val operationType: OperationType,
    val onCloseRequest: () -> Unit,
) {

    sealed class OperationType {

        object New : OperationType()

        data class NewFromExisting(val id: Long) : OperationType()

        data class EditExisting(val id: Long) : OperationType()
    }
}
