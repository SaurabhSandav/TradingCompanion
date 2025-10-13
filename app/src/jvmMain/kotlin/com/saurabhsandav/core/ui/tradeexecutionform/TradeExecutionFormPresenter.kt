package com.saurabhsandav.core.ui.tradeexecutionform

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.trading.ProfileId
import com.saurabhsandav.core.trading.TradingProfiles
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormModel
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormState
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType.AddToTrade
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType.CloseTrade
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType.Edit
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType.New
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType.NewFromExisting
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType.NewFromExistingInTrade
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType.NewSized
import com.saurabhsandav.core.utils.launchUnit
import com.saurabhsandav.kbigdecimal.toKBigDecimal
import com.saurabhsandav.trading.market.india.FinvasiaBroker
import com.saurabhsandav.trading.record.model.TradeExecutionId
import com.saurabhsandav.trading.record.model.TradeExecutionSide
import com.saurabhsandav.trading.record.model.TradeId
import com.saurabhsandav.trading.record.model.TradeSide
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

@AssistedInject
internal class TradeExecutionFormPresenter(
    @Assisted private val onCloseRequest: () -> Unit,
    @Assisted private val coroutineScope: CoroutineScope,
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
            is NewSized -> newSized(formType.formModel)
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

        val title = remember {
            when (formType) {
                is Edit -> "Edit Trade Execution (${formType.id})"
                else -> "New Trade Execution"
            }
        }

        return@launchMolecule TradeExecutionFormState(
            title = "${tradingProfileName}$title",
            isSymbolEditable = remember {
                !(formType is NewFromExistingInTrade || formType is AddToTrade || formType is CloseTrade)
            },
            isSideSelectable = remember { !(formType is AddToTrade || formType is CloseTrade) },
            formModel = formModel,
            onSubmit = ::onSubmit,
        )
    }

    private fun onSubmit() = coroutineScope.launchUnit {

        val formModel = formModel!!
        val tz = TimeZone.currentSystemDefault()

        val executionId = when (formType) {
            is Edit -> {

                tradingRecord.await().executions.edit(
                    id = formType.id,
                    brokerId = FinvasiaBroker.Id,
                    instrument = formModel.instrumentField.value!!,
                    symbolId = formModel.symbolField.value!!,
                    quantity = formModel.quantityField.value.toKBigDecimal(),
                    lots = formModel.lotsField.value.ifBlank { null }?.toInt(),
                    side = if (formModel.isBuyField.value) TradeExecutionSide.Buy else TradeExecutionSide.Sell,
                    price = formModel.priceField.value.toKBigDecimal(),
                    timestamp = formModel.timestamp.toInstant(tz),
                )

                formType.id
            }

            else -> tradingRecord.await().executions.new(
                brokerId = FinvasiaBroker.Id,
                instrument = formModel.instrumentField.value!!,
                symbolId = formModel.symbolField.value!!,
                quantity = formModel.quantityField.value.toKBigDecimal(),
                lots = formModel.lotsField.value.ifBlank { null }?.toInt(),
                side = if (formModel.isBuyField.value) TradeExecutionSide.Buy else TradeExecutionSide.Sell,
                price = formModel.priceField.value.toKBigDecimal(),
                timestamp = formModel.timestamp.toInstant(tz),
                locked = false,
            )
        }

        if (formType is NewSized) {

            // Single execution can close a trade and open a new one.
            // Make sure to choose the open trade
            val trade = tradingRecord.await().trades.getForExecution(executionId).first().single { !it.isClosed }

            // Add stop
            if (formModel.addStopField.value) tradingRecord.await().stops.add(trade.id, formType.stop)

            // Add target
            if (formModel.addTargetField.value) tradingRecord.await().targets.add(trade.id, formType.target)
        }

        // Close form
        onCloseRequest()
    }

    private fun new() {

        formModel = TradeExecutionFormModel()
    }

    private fun newFromExisting(id: TradeExecutionId) = coroutineScope.launchUnit {

        val execution = tradingRecord.await().executions.getById(id).first()

        formModel = TradeExecutionFormModel(
            instrument = execution.instrument,
            symbolId = execution.symbolId,
            quantity = execution.quantity.toString(),
            lots = execution.lots?.toString() ?: "",
            isBuy = execution.side == TradeExecutionSide.Buy,
            price = execution.price.toString(),
        )
    }

    private fun newSized(initialModel: TradeExecutionFormModel) {

        formModel = initialModel
    }

    private fun addToTrade(tradeId: TradeId) = coroutineScope.launchUnit {

        val trade = tradingRecord.await().trades.getById(tradeId).first()

        formModel = TradeExecutionFormModel(
            instrument = trade.instrument,
            symbolId = trade.symbolId,
            isBuy = trade.side == TradeSide.Long,
        )
    }

    private fun closeTrade(tradeId: TradeId) = coroutineScope.launchUnit {

        val trade = tradingRecord.await().trades.getById(tradeId).first()

        formModel = TradeExecutionFormModel(
            instrument = trade.instrument,
            symbolId = trade.symbolId,
            quantity = (trade.quantity - trade.closedQuantity).toString(),
            isBuy = trade.side != TradeSide.Long,
        )
    }

    private fun edit(id: TradeExecutionId) = coroutineScope.launchUnit {

        val execution = tradingRecord.await().executions.getById(id).first()

        formModel = TradeExecutionFormModel(
            instrument = execution.instrument,
            symbolId = execution.symbolId,
            quantity = execution.quantity.toString(),
            lots = execution.lots?.toString() ?: "",
            isBuy = execution.side == TradeExecutionSide.Buy,
            price = execution.price.toString(),
            timestamp = execution.timestamp.toLocalDateTime(TimeZone.currentSystemDefault()),
        )
    }

    @AssistedFactory
    fun interface Factory {

        fun create(
            onCloseRequest: () -> Unit,
            coroutineScope: CoroutineScope,
        ): TradeExecutionFormPresenter
    }
}
