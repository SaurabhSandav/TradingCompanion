package com.saurabhsandav.core.ui.tradeexecutionform

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.*
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormModel
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormState
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType.*
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

internal class TradeExecutionFormPresenter(
    private val onCloseRequest: () -> Unit,
    private val coroutineScope: CoroutineScope,
    private val profileId: ProfileId,
    private val formType: TradeExecutionFormType,
    private val tradingProfiles: TradingProfiles,
) {

    private val tradingRecord = coroutineScope.async { tradingProfiles.getRecord(profileId) }
    private var formModel by mutableStateOf<TradeExecutionFormModel?>(null)

    init {

        when (formType) {
            is New -> new()
            is NewFromExisting -> newFromExisting(formType.id)
            is NewFromExistingInTrade -> newFromExisting(formType.id)
            is NewSized -> newSized(formType.initialModel)
            is AddToTrade -> addToTrade(formType.tradeId)
            is CloseTrade -> closeTrade(formType.tradeId)
            is Edit -> edit(formType.id)
        }

        // Close if profile deleted
        tradingProfiles
            .getProfileOrNull(profileId)
            .filter { it == null }
            .onEach { onCloseRequest() }
            .launchIn(coroutineScope)
    }

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        val tradingProfileName by remember {
            tradingProfiles.getProfileOrNull(profileId).filterNotNull().map { profile -> "${profile.name} - " }
        }.collectAsState("")

        return@launchMolecule TradeExecutionFormState(
            title = "${tradingProfileName}${if (formType is Edit) "Edit Trade Execution (${formType.id})" else "New Trade Execution"}",
            formModel = formModel,
        )
    }

    private suspend fun TradeExecutionFormModel.onSaveExecution() {

        val tz = TimeZone.currentSystemDefault()

        val executionId = when (formType) {
            is Edit -> {

                tradingRecord.await().executions.edit(
                    id = formType.id,
                    broker = "Finvasia",
                    instrument = instrumentField.value!!,
                    ticker = tickerField.value!!,
                    quantity = quantityField.value.toBigDecimal(),
                    lots = lotsField.value.ifBlank { null }?.toInt(),
                    side = if (isBuyField.value) TradeExecutionSide.Buy else TradeExecutionSide.Sell,
                    price = priceField.value.toBigDecimal(),
                    timestamp = timestamp.toInstant(tz),
                )

                formType.id
            }

            else -> tradingRecord.await().executions.new(
                broker = "Finvasia",
                instrument = instrumentField.value!!,
                ticker = tickerField.value!!,
                quantity = quantityField.value.toBigDecimal(),
                lots = lotsField.value.ifBlank { null }?.toInt(),
                side = if (isBuyField.value) TradeExecutionSide.Buy else TradeExecutionSide.Sell,
                price = priceField.value.toBigDecimal(),
                timestamp = timestamp.toInstant(tz),
                locked = false,
            )
        }

        if (formType is NewSized) {

            // Single execution can close a trade and open a new one.
            // Make sure to choose the open trade
            val trade = tradingRecord.await().trades.getForExecution(executionId).first().single { !it.isClosed }

            // Add stop
            if (addStopField.value) tradingRecord.await().stops.add(trade.id, formType.stop)

            // Add target
            if (addTargetField.value) tradingRecord.await().targets.add(trade.id, formType.target)
        }

        // Close form
        onCloseRequest()
    }

    private fun new() {

        setFormModel(initial = TradeExecutionFormModel.Initial())
    }

    private fun newFromExisting(id: TradeExecutionId) = coroutineScope.launchUnit {

        val execution = tradingRecord.await().executions.getById(id).first()

        setFormModel(
            initial = TradeExecutionFormModel.Initial(
                instrument = execution.instrument,
                ticker = execution.ticker,
                quantity = execution.quantity.toString(),
                lots = execution.lots?.toString() ?: "",
                isBuy = execution.side == TradeExecutionSide.Buy,
                price = execution.price.toPlainString(),
            ),
        )
    }

    private fun newSized(initialModel: TradeExecutionFormModel.Initial) {

        setFormModel(initialModel)
    }

    private fun addToTrade(tradeId: TradeId) = coroutineScope.launchUnit {

        val trade = tradingRecord.await().trades.getById(tradeId).first()

        setFormModel(
            initial = TradeExecutionFormModel.Initial(
                instrument = trade.instrument,
                ticker = trade.ticker,
                isBuy = trade.side == TradeSide.Long,
            ),
        )
    }

    private fun closeTrade(tradeId: TradeId) = coroutineScope.launchUnit {

        val trade = tradingRecord.await().trades.getById(tradeId).first()

        setFormModel(
            initial = TradeExecutionFormModel.Initial(
                instrument = trade.instrument,
                ticker = trade.ticker,
                quantity = (trade.quantity - trade.closedQuantity).toPlainString(),
                isBuy = trade.side != TradeSide.Long,
            ),
        )
    }

    private fun edit(id: TradeExecutionId) = coroutineScope.launchUnit {

        val execution = tradingRecord.await().executions.getById(id).first()

        setFormModel(
            initial = TradeExecutionFormModel.Initial(
                instrument = execution.instrument,
                ticker = execution.ticker,
                quantity = execution.quantity.toString(),
                lots = execution.lots?.toString() ?: "",
                isBuy = execution.side == TradeExecutionSide.Buy,
                price = execution.price.toPlainString(),
                timestamp = execution.timestamp.toLocalDateTime(TimeZone.currentSystemDefault()),
            ),
        )
    }

    private fun setFormModel(initial: TradeExecutionFormModel.Initial) {

        formModel = TradeExecutionFormModel(
            coroutineScope = coroutineScope,
            initial = initial,
            onSubmit = { onSaveExecution() },
        )
    }
}
