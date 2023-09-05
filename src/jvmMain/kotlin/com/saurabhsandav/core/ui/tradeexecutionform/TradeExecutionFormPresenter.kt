package com.saurabhsandav.core.ui.tradeexecutionform

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.Instrument
import com.saurabhsandav.core.trades.model.TradeExecutionSide
import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.ui.tradeexecutionform.model.OrderFormModel
import com.saurabhsandav.core.ui.tradeexecutionform.model.OrderFormState
import com.saurabhsandav.core.ui.tradeexecutionform.model.OrderFormType
import com.saurabhsandav.core.ui.tradeexecutionform.model.OrderFormType.*
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.nanoseconds

@Stable
internal class OrderFormPresenter(
    private val coroutineScope: CoroutineScope,
    private val profileId: Long,
    private val formType: OrderFormType,
    private val appModule: AppModule,
    private val tradingProfiles: TradingProfiles = appModule.tradingProfiles,
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

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        val tradingProfileName by remember {
            tradingProfiles.getProfile(profileId).map { profile -> "${profile.name} - " }
        }.collectAsState("")

        return@launchMolecule OrderFormState(
            title = "${tradingProfileName}${if (formType is Edit) "Edit Order (${formType.id})" else "New Order"}",
            formModel = formModel,
            onSaveOrder = ::onSaveOrder,
        )
    }

    private fun onSaveOrder() = coroutineScope.launchUnit {

        if (!formValidator.isValid()) return@launchUnit

        val formModel = requireNotNull(formModel)

        val tradingRecord = tradingProfiles.getRecord(profileId)

        val orderId = when (formType) {
            is Edit -> tradingRecord.executions.edit(
                id = formType.id,
                broker = "Finvasia",
                instrument = Instrument.fromString(formModel.instrument.value!!),
                ticker = formModel.ticker.value!!,
                quantity = formModel.quantity.value.toBigDecimal(),
                lots = formModel.lots.value.ifBlank { null }?.toInt(),
                side = if (formModel.isBuy.value) TradeExecutionSide.Buy else TradeExecutionSide.Sell,
                price = formModel.price.value.toBigDecimal(),
                timestamp = formModel.timestamp.value,
            )

            else -> tradingRecord.executions.new(
                broker = "Finvasia",
                instrument = Instrument.fromString(formModel.instrument.value!!),
                ticker = formModel.ticker.value!!,
                quantity = formModel.quantity.value.toBigDecimal(),
                lots = formModel.lots.value.ifBlank { null }?.toInt(),
                side = if (formModel.isBuy.value) TradeExecutionSide.Buy else TradeExecutionSide.Sell,
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
            instrument = null,
            ticker = null,
            quantity = "",
            lots = "",
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

        val tradingRecord = tradingProfiles.getRecord(profileId)

        val order = tradingRecord.executions.getById(id).first()

        formModel = OrderFormModel(
            validator = formValidator,
            instrument = order.instrument.strValue,
            ticker = order.ticker,
            quantity = order.quantity.toString(),
            lots = order.lots?.toString() ?: "",
            isBuy = order.side == TradeExecutionSide.Buy,
            price = order.price.toPlainString(),
            timestamp = run {
                val currentTime = Clock.System.now()
                val currentTimeWithoutNanoseconds = currentTime - currentTime.nanosecondsOfSecond.nanoseconds
                currentTimeWithoutNanoseconds.toLocalDateTime(TimeZone.currentSystemDefault())
            },
        )
    }

    private fun editOrder(id: Long) = coroutineScope.launchUnit {

        val tradingRecord = tradingProfiles.getRecord(profileId)

        val order = tradingRecord.executions.getById(id).first()

        formModel = OrderFormModel(
            validator = formValidator,
            instrument = order.instrument.strValue,
            ticker = order.ticker,
            quantity = order.quantity.toString(),
            lots = order.lots?.toString() ?: "",
            isBuy = order.side == TradeExecutionSide.Buy,
            price = order.price.toPlainString(),
            timestamp = order.timestamp,
        )
    }
}
