package com.saurabhsandav.core.ui.tradeexecutionform

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.*
import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormModel
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormState
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType.*
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

@Stable
internal class TradeExecutionFormPresenter(
    private val onCloseRequest: () -> Unit,
    private val coroutineScope: CoroutineScope,
    private val profileId: ProfileId,
    private val formType: TradeExecutionFormType,
    private val tradingProfiles: TradingProfiles,
) {

    private val formValidator = FormValidator(coroutineScope)
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
    }

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        val tradingProfileName by remember {
            tradingProfiles.getProfile(profileId).map { profile -> "${profile.name} - " }
        }.collectAsState("")

        return@launchMolecule TradeExecutionFormState(
            title = "${tradingProfileName}${if (formType is Edit) "Edit Trade Execution (${formType.id})" else "New Trade Execution"}",
            formModel = formModel,
            onSaveExecution = ::onSaveExecution,
        )
    }

    private fun onSaveExecution() = coroutineScope.launchUnit {

        if (!formValidator.validate()) return@launchUnit

        val formModel = requireNotNull(formModel)

        val tradingRecord = tradingProfiles.getRecord(profileId)

        val tz = TimeZone.currentSystemDefault()

        val executionId = when (formType) {
            is Edit -> tradingRecord.executions.edit(
                id = formType.id,
                broker = "Finvasia",
                instrument = formModel.instrumentField.value!!,
                ticker = formModel.tickerField.value!!,
                quantity = formModel.quantityField.value.toBigDecimal(),
                lots = formModel.lotsField.value.ifBlank { null }?.toInt(),
                side = if (formModel.isBuyField.value) TradeExecutionSide.Buy else TradeExecutionSide.Sell,
                price = formModel.priceField.value.toBigDecimal(),
                timestamp = formModel.timestamp.toInstant(tz),
            )

            else -> tradingRecord.executions.new(
                broker = "Finvasia",
                instrument = formModel.instrumentField.value!!,
                ticker = formModel.tickerField.value!!,
                quantity = formModel.quantityField.value.toBigDecimal(),
                lots = formModel.lotsField.value.ifBlank { null }?.toInt(),
                side = if (formModel.isBuyField.value) TradeExecutionSide.Buy else TradeExecutionSide.Sell,
                price = formModel.priceField.value.toBigDecimal(),
                timestamp = formModel.timestamp.toInstant(tz),
                locked = false,
            )
        }

        if (formType is NewSized) {

            // Single execution can close a trade and open a new one.
            // Make sure to choose the open trade
            val trade = tradingRecord.trades.getTradesForExecution(executionId).first().single { !it.isClosed }

            // Add stop
            if (formModel.addStopField.value) tradingRecord.trades.addStop(trade.id, formType.stop)

            // Add target
            if (formModel.addTargetField.value) tradingRecord.trades.addTarget(trade.id, formType.target)
        }

        // Close form
        onCloseRequest()
    }

    private fun new() {

        this.formModel = TradeExecutionFormModel(
            validator = formValidator,
            initial = TradeExecutionFormModel.Initial(),
        )
    }

    private fun newFromExisting(id: TradeExecutionId) = coroutineScope.launchUnit {

        val tradingRecord = tradingProfiles.getRecord(profileId)

        val execution = tradingRecord.executions.getById(id).first()

        formModel = TradeExecutionFormModel(
            validator = formValidator,
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

        this.formModel = TradeExecutionFormModel(
            validator = formValidator,
            initial = initialModel,
        )
    }

    private fun addToTrade(tradeId: TradeId) = coroutineScope.launchUnit {

        val tradingRecord = tradingProfiles.getRecord(profileId)

        val trade = tradingRecord.trades.getById(tradeId).first()

        formModel = TradeExecutionFormModel(
            validator = formValidator,
            initial = TradeExecutionFormModel.Initial(
                instrument = trade.instrument,
                ticker = trade.ticker,
                isBuy = trade.side == TradeSide.Long,
            ),
        )
    }

    private fun closeTrade(tradeId: TradeId) = coroutineScope.launchUnit {

        val tradingRecord = tradingProfiles.getRecord(profileId)

        val trade = tradingRecord.trades.getById(tradeId).first()

        formModel = TradeExecutionFormModel(
            validator = formValidator,
            initial = TradeExecutionFormModel.Initial(
                instrument = trade.instrument,
                ticker = trade.ticker,
                quantity = (trade.quantity - trade.closedQuantity).toPlainString(),
                isBuy = trade.side != TradeSide.Long,
            ),
        )
    }

    private fun edit(id: TradeExecutionId) = coroutineScope.launchUnit {

        val tradingRecord = tradingProfiles.getRecord(profileId)

        val execution = tradingRecord.executions.getById(id).first()

        formModel = TradeExecutionFormModel(
            validator = formValidator,
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
}
