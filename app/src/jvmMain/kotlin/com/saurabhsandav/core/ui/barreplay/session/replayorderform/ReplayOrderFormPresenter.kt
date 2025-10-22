package com.saurabhsandav.core.ui.barreplay.session.replayorderform

import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.ui.barreplay.session.ReplayOrdersManager
import com.saurabhsandav.core.ui.barreplay.session.replayorderform.model.ReplayOrderFormModel
import com.saurabhsandav.core.ui.barreplay.session.replayorderform.model.ReplayOrderFormState
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.kbigdecimal.toKBigDecimal
import com.saurabhsandav.kbigdecimal.toKBigDecimalOrNull
import com.saurabhsandav.trading.record.model.TradeExecutionSide
import kotlinx.coroutines.CoroutineScope

internal class ReplayOrderFormPresenter(
    coroutineScope: CoroutineScope,
    private val replayOrdersManager: ReplayOrdersManager,
    private val stockChartParams: StockChartParams,
    initialModel: ReplayOrderFormModel,
    private val onOrderSaved: () -> Unit,
) {

    private val formModel = initialModel

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule ReplayOrderFormState(
            title = "New Order",
            ticker = stockChartParams.symbolId.value,
            formModel = formModel,
            onSubmit = ::onSubmit,
        )
    }

    private fun onSubmit() {

        replayOrdersManager.newOrder(
            stockChartParams = stockChartParams,
            quantity = formModel.quantityField.value.toKBigDecimal(),
            lots = formModel.lotsField.value.ifBlank { null }?.toInt() ?: formModel.quantityField.value.toInt(),
            side = if (formModel.isBuyField.value) TradeExecutionSide.Buy else TradeExecutionSide.Sell,
            price = formModel.priceField.value.toKBigDecimal(),
            stop = formModel.stop.value.toKBigDecimalOrNull(),
            target = formModel.target.value.toKBigDecimalOrNull(),
        )

        // Notify order saved
        onOrderSaved.invoke()
    }
}
