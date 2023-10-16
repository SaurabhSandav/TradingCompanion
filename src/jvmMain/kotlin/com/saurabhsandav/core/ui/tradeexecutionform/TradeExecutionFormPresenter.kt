package com.saurabhsandav.core.ui.tradeexecutionform

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.TradeExecutionSide
import com.saurabhsandav.core.trades.model.TradeSide
import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormModel
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormState
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType.*
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Stable
internal class TradeExecutionFormPresenter(
    private val coroutineScope: CoroutineScope,
    private val profileId: Long,
    private val formType: TradeExecutionFormType,
    private val appModule: AppModule,
    private val tradingProfiles: TradingProfiles = appModule.tradingProfiles,
    private val onExecutionSaved: ((executionId: Long) -> Unit)? = null,
) {

    private val formValidator = FormValidator(coroutineScope)
    private var formModel by mutableStateOf<TradeExecutionFormModel?>(null)

    init {

        when (formType) {
            is New -> new(formType.initialModel)
            is NewFromExisting -> newFromExisting(formType.id)
            is NewFromExistingInTrade -> newFromExisting(formType.id)
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
                timestamp = formModel.timestamp,
            )

            else -> tradingRecord.executions.new(
                broker = "Finvasia",
                instrument = formModel.instrumentField.value!!,
                ticker = formModel.tickerField.value!!,
                quantity = formModel.quantityField.value.toBigDecimal(),
                lots = formModel.lotsField.value.ifBlank { null }?.toInt(),
                side = if (formModel.isBuyField.value) TradeExecutionSide.Buy else TradeExecutionSide.Sell,
                price = formModel.priceField.value.toBigDecimal(),
                timestamp = formModel.timestamp,
                locked = false,
            )
        }

        // Notify execution saved
        onExecutionSaved?.invoke(executionId)
    }

    private fun new(initialModel: TradeExecutionFormModel.Initial?) {

        this.formModel = TradeExecutionFormModel(
            validator = formValidator,
            initial = initialModel ?: TradeExecutionFormModel.Initial(),
        )
    }

    private fun newFromExisting(id: Long) = coroutineScope.launchUnit {

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

    private fun addToTrade(tradeId: Long) = coroutineScope.launchUnit {

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

    private fun closeTrade(tradeId: Long) = coroutineScope.launchUnit {

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

    private fun edit(id: Long) = coroutineScope.launchUnit {

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
                timestamp = execution.timestamp,
            ),
        )
    }
}
