package com.saurabhsandav.core.ui.tradeorderform

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trades.TradeOrdersRepo
import com.saurabhsandav.core.trades.model.OrderType
import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.ui.tradeorderform.model.OrderFormModel
import com.saurabhsandav.core.ui.tradeorderform.model.OrderFormState
import com.saurabhsandav.core.ui.tradeorderform.model.OrderFormType
import com.saurabhsandav.core.ui.tradeorderform.model.OrderFormType.*
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.nanoseconds

@Stable
internal class OrderFormPresenter(
    private val coroutineScope: CoroutineScope,
    private val formType: OrderFormType,
    private val appModule: AppModule,
    private val tradeOrdersRepo: TradeOrdersRepo = appModule.tradeOrdersRepo,
    private val onOrderSaved: ((orderId: Long) -> Unit)? = null,
) {

    private val formValidator = FormValidator()
    private var formModel by mutableStateOf<OrderFormModel?>(null)

    init {

        when (formType) {
            is New -> new(formType.formModel)
            is NewFromExisting -> newFromExisting(formType.id)
            is Edit -> editOrder(formType.id)
        }
    }

    val state = coroutineScope.launchMolecule(RecompositionClock.ContextClock) {

        return@launchMolecule OrderFormState(
            title = when (formType) {
                is New, is NewFromExisting -> "New Order"
                is Edit -> "Edit Order"
            },
            formModel = formModel,
            onSaveOrder = ::onSaveOrder,
        )
    }

    private fun onSaveOrder() = coroutineScope.launchUnit {

        if (!formValidator.isValid()) return@launchUnit

        val formModel = requireNotNull(formModel)

        val orderId = when (formType) {
            is Edit -> tradeOrdersRepo.edit(
                id = formType.id,
                broker = "Finvasia",
                ticker = formModel.ticker.value!!,
                quantity = formModel.quantity.value.toBigDecimal(),
                lots = null,
                type = if (formModel.isBuy.value) OrderType.Buy else OrderType.Sell,
                price = formModel.price.value.toBigDecimal(),
                timestamp = formModel.timestamp.value,
            )

            else -> tradeOrdersRepo.new(
                broker = "Finvasia",
                ticker = formModel.ticker.value!!,
                quantity = formModel.quantity.value.toBigDecimal(),
                lots = null,
                type = if (formModel.isBuy.value) OrderType.Buy else OrderType.Sell,
                price = formModel.price.value.toBigDecimal(),
                timestamp = formModel.timestamp.value,
                locked = false,
            )
        }

        // Notify order saved
        onOrderSaved?.invoke(orderId)
    }

    private fun new(formModel: ((FormValidator) -> OrderFormModel)? = null) {

        this.formModel = formModel?.invoke(formValidator) ?: OrderFormModel(
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
    }

    private fun newFromExisting(id: Long) = coroutineScope.launchUnit {

        val order = tradeOrdersRepo.getById(id).first()

        formModel = OrderFormModel(
            validator = formValidator,
            ticker = order.ticker,
            quantity = order.quantity.toString(),
            isBuy = order.type == OrderType.Buy,
            price = order.price.toPlainString(),
            timestamp = run {
                val currentTime = Clock.System.now()
                val currentTimeWithoutNanoseconds = currentTime - currentTime.nanosecondsOfSecond.nanoseconds
                currentTimeWithoutNanoseconds.toLocalDateTime(TimeZone.currentSystemDefault())
            },
        )
    }

    private fun editOrder(id: Long) = coroutineScope.launchUnit {

        val order = tradeOrdersRepo.getById(id).first()

        formModel = OrderFormModel(
            validator = formValidator,
            ticker = order.ticker,
            quantity = order.quantity.toString(),
            isBuy = order.type == OrderType.Buy,
            price = order.price.toPlainString(),
            timestamp = order.timestamp,
        )
    }
}
