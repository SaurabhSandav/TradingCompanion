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
    initialModel: ReplayOrderFormModel?,
    private val onOrderSaved: () -> Unit,
) {

    private var formModel by mutableStateOf<ReplayOrderFormModel?>(null)

    init {

        this.formModel = initialModel ?: ReplayOrderFormModel()
    }

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule ReplayOrderFormState(
            title = "New Order",
            ticker = stockChartParams.ticker,
            formModel = formModel,
            onSubmit = ::onSubmit,
        )
    }

    private fun onSubmit() {

        val formModel = formModel!!

        replayOrdersManager.newOrder(
            stockChartParams = stockChartParams,
            quantity = formModel.quantityField.value.toBigDecimal(),
            side = if (formModel.isBuyField.value) TradeExecutionSide.Buy else TradeExecutionSide.Sell,
            price = formModel.priceField.value.toBigDecimal(),
            stop = formModel.stop.value.toBigDecimalOrNull(),
            target = formModel.target.value.toBigDecimalOrNull(),
        )

        // Notify order saved
        onOrderSaved.invoke()
    }
}
