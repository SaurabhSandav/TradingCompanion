package com.saurabhsandav.core.ui.barreplay.session.replayorderform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.trading.SymbolsProvider
import com.saurabhsandav.core.trading.getSymbolOrError
import com.saurabhsandav.core.ui.barreplay.session.ReplayOrdersManager
import com.saurabhsandav.core.ui.barreplay.session.replayorderform.model.ReplayOrderFormModel
import com.saurabhsandav.core.ui.barreplay.session.replayorderform.model.ReplayOrderFormState
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.kbigdecimal.toKBigDecimal
import com.saurabhsandav.kbigdecimal.toKBigDecimalOrNull
import com.saurabhsandav.trading.core.Instrument
import com.saurabhsandav.trading.market.india.FinvasiaBroker
import com.saurabhsandav.trading.record.model.TradeExecutionSide
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first

@AssistedInject
internal class ReplayOrderFormPresenter(
    @Assisted coroutineScope: CoroutineScope,
    @Assisted private val stockChartParams: StockChartParams,
    @Assisted initialModel: ReplayOrderFormModel,
    @Assisted private val onOrderSaved: () -> Unit,
    private val replayOrdersManager: ReplayOrdersManager,
    private val symbolsProvider: SymbolsProvider,
) {

    private val formModel = initialModel
    private val brokerId = FinvasiaBroker.Id

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule ReplayOrderFormState(
            title = "New Order",
            ticker = stockChartParams.symbolId.value,
            formModel = formModel,
            showLots = showLots(),
            onSubmit = ::onSubmit,
        )
    }

    @Composable
    private fun showLots(): Boolean = produceState(false) {

        val symbol = symbolsProvider.getSymbolOrError(brokerId, stockChartParams.symbolId).first()

        value = symbol.instrument == Instrument.Futures || symbol.instrument == Instrument.Options
    }.value

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

    @AssistedFactory
    fun interface Factory {

        fun create(
            coroutineScope: CoroutineScope,
            stockChartParams: StockChartParams,
            initialModel: ReplayOrderFormModel,
            onOrderSaved: () -> Unit,
        ): ReplayOrderFormPresenter
    }
}
