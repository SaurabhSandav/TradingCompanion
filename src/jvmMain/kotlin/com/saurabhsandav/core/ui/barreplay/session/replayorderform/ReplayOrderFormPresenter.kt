package com.saurabhsandav.core.ui.barreplay.session.replayorderform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.trades.model.TradeExecutionSide
import com.saurabhsandav.core.ui.barreplay.session.ReplayOrdersManager
import com.saurabhsandav.core.ui.barreplay.session.replayorderform.model.ReplayOrderFormModel
import com.saurabhsandav.core.ui.barreplay.session.replayorderform.model.ReplayOrderFormState
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import kotlinx.coroutines.CoroutineScope

internal class ReplayOrderFormPresenter(
    coroutineScope: CoroutineScope,
    private val replayOrdersManager: ReplayOrdersManager,
    private val stockChartParams: StockChartParams,
    initialModel: ReplayOrderFormModel.Initial?,
    private val onOrderSaved: () -> Unit,
) {

    private var formModel by mutableStateOf<ReplayOrderFormModel?>(null)

    init {

        this.formModel = ReplayOrderFormModel(
            coroutineScope = coroutineScope,
            initial = initialModel ?: ReplayOrderFormModel.Initial(),
            onSubmit = { onSaveOrder() },
        )
    }

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule ReplayOrderFormState(
            title = "New Order",
            ticker = stockChartParams.ticker,
            formModel = formModel,
        )
    }

    private fun ReplayOrderFormModel.onSaveOrder() {

        replayOrdersManager.newOrder(
            stockChartParams = stockChartParams,
            quantity = quantityField.value.toBigDecimal(),
            side = if (isBuyField.value) TradeExecutionSide.Buy else TradeExecutionSide.Sell,
            price = priceField.value.toBigDecimal(),
            stop = stop.value.toBigDecimalOrNull(),
            target = target.value.toBigDecimalOrNull(),
        )

        // Notify order saved
        onOrderSaved.invoke()
    }
}
