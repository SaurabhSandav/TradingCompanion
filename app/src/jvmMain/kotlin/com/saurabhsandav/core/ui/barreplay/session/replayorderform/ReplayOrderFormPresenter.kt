package com.saurabhsandav.core.ui.barreplay.session.replayorderform

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.snapshotFlow
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.CachedSymbol
import com.saurabhsandav.core.trading.SymbolsProvider
import com.saurabhsandav.core.trading.getSymbolOrError
import com.saurabhsandav.core.ui.barreplay.session.ReplayOrdersManager
import com.saurabhsandav.core.ui.barreplay.session.replayorderform.model.ReplayOrderFormEvent
import com.saurabhsandav.core.ui.barreplay.session.replayorderform.model.ReplayOrderFormEvent.SetQuantityActiveField
import com.saurabhsandav.core.ui.barreplay.session.replayorderform.model.ReplayOrderFormEvent.Submit
import com.saurabhsandav.core.ui.barreplay.session.replayorderform.model.ReplayOrderFormModel
import com.saurabhsandav.core.ui.barreplay.session.replayorderform.model.ReplayOrderFormState
import com.saurabhsandav.core.ui.barreplay.session.replayorderform.model.ReplayOrderFormState.QuantityActiveField
import com.saurabhsandav.core.ui.barreplay.session.replayorderform.model.ReplayOrderFormState.QuantityActiveField.Lots
import com.saurabhsandav.core.ui.barreplay.session.replayorderform.model.ReplayOrderFormState.QuantityActiveField.Quantity
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.kbigdecimal.toKBigDecimal
import com.saurabhsandav.kbigdecimal.toKBigDecimalOrNull
import com.saurabhsandav.trading.core.Instrument
import com.saurabhsandav.trading.core.SymbolId
import com.saurabhsandav.trading.market.india.FinvasiaBroker
import com.saurabhsandav.trading.record.model.TradeExecutionSide
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
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
    private var symbol: CachedSymbol? = null
    private var quantityActiveField: QuantityActiveField = Quantity

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        syncQuantityAndLots()

        return@launchMolecule ReplayOrderFormState(
            title = "New Order",
            ticker = stockChartParams.symbolId.value,
            formModel = formModel,
            showLots = showLots(),
            eventSink = ::onEvent,
        )
    }

    @Composable
    private fun syncQuantityAndLots() {

        LaunchedEffect(Unit) {

            val symbol = getSymbol(stockChartParams.symbolId)

            snapshotFlow {
                formModel.quantityField.value to formModel.lotsField.value
            }.collectLatest {

                when (quantityActiveField) {
                    Quantity -> {
                        val (quantity, isValid) = formModel.quantityField.validate()
                        if (!isValid) return@collectLatest
                        val lotSize = symbol.lotSize
                        val lots = (quantity.toKBigDecimal() / lotSize).toString()
                        formModel.lotsField.holder.setTextAndPlaceCursorAtEnd(lots)
                    }

                    Lots -> {
                        val (lots, isValid) = formModel.lotsField.validate()
                        if (!isValid || lots.isEmpty()) return@collectLatest
                        val lotSize = symbol.lotSize
                        val quantity = (lots.toKBigDecimal() * lotSize).toString()
                        formModel.quantityField.holder.setTextAndPlaceCursorAtEnd(quantity)
                    }
                }
            }
        }
    }

    private fun onEvent(event: ReplayOrderFormEvent) {

        when (event) {
            is SetQuantityActiveField -> onSetQuantityActiveField(event.activeField)
            Submit -> onSubmit()
        }
    }

    private fun onSetQuantityActiveField(activeField: QuantityActiveField) {
        quantityActiveField = activeField
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
            lots = formModel.lotsField.value.toInt(),
            side = if (formModel.isBuyField.value) TradeExecutionSide.Buy else TradeExecutionSide.Sell,
            price = formModel.priceField.value.toKBigDecimal(),
            stop = formModel.stop.value.toKBigDecimalOrNull(),
            target = formModel.target.value.toKBigDecimalOrNull(),
        )

        // Notify order saved
        onOrderSaved.invoke()
    }

    private suspend fun getSymbol(symbolId: SymbolId): CachedSymbol {
        this.symbol = symbol ?: symbolsProvider.getSymbolOrError(brokerId, symbolId).first()
        return this.symbol!!
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
