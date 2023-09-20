package com.saurabhsandav.core.ui.barreplay.session.replayorderform

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.trades.model.TradeExecutionSide
import com.saurabhsandav.core.ui.barreplay.session.ReplayOrdersManager
import com.saurabhsandav.core.ui.barreplay.session.replayorderform.model.ReplayOrderFormModel
import com.saurabhsandav.core.ui.barreplay.session.replayorderform.model.ReplayOrderFormState
import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.coroutines.CoroutineScope

@Stable
internal class ReplayOrderFormPresenter(
    private val coroutineScope: CoroutineScope,
    private val replayOrdersManager: ReplayOrdersManager,
    initialFormModel: ((FormValidator) -> ReplayOrderFormModel)? = null,
    private val onOrderSaved: ((orderId: Long) -> Unit)? = null,
) {

    private val formValidator = FormValidator()
    private var formModel by mutableStateOf<ReplayOrderFormModel?>(null)

    init {

        this.formModel = initialFormModel?.invoke(formValidator) ?: ReplayOrderFormModel(
            validator = formValidator,
            instrument = null,
            ticker = null,
            quantity = "",
            lots = "",
            isBuy = true,
            price = "",
            stop = "",
            target = "",
        )
    }

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule ReplayOrderFormState(
            title = "New Order",
            formModel = formModel,
            onSaveOrder = ::onSaveOrder,
        )
    }

    private fun onSaveOrder() = coroutineScope.launchUnit {

        if (!formValidator.isValid()) return@launchUnit

        val formModel = requireNotNull(formModel)

        val orderId = replayOrdersManager.newOrder(
            broker = "Finvasia",
            instrument = formModel.instrument.value!!,
            ticker = formModel.ticker.value!!,
            quantity = formModel.quantity.value.toBigDecimal(),
            lots = formModel.lots.value.ifBlank { null }?.toInt(),
            side = if (formModel.isBuy.value) TradeExecutionSide.Buy else TradeExecutionSide.Sell,
            price = formModel.price.value.toBigDecimal(),
            stop = formModel.stop.value.toBigDecimalOrNull(),
            target = formModel.target.value.toBigDecimalOrNull(),
        )

        // Notify order saved
        onOrderSaved?.invoke(orderId)
    }
}
