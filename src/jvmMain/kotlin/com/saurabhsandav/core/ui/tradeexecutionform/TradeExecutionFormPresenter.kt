package com.saurabhsandav.core.ui.tradeexecutionform

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.TradeExecutionSide
import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormModel
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormState
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType.*
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.nanoseconds

@Stable
internal class TradeExecutionFormPresenter(
    private val coroutineScope: CoroutineScope,
    private val profileId: Long,
    private val formType: TradeExecutionFormType,
    private val appModule: AppModule,
    private val tradingProfiles: TradingProfiles = appModule.tradingProfiles,
    private val onExecutionSaved: ((executionId: Long) -> Unit)? = null,
) {

    private val formValidator = FormValidator()
    private var formModel by mutableStateOf<TradeExecutionFormModel?>(null)

    init {

        when (formType) {
            is New -> new(formType.formModel)
            is NewFromExisting -> newFromExisting(formType.id)
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

        if (!formValidator.isValid()) return@launchUnit

        val formModel = requireNotNull(formModel)

        val tradingRecord = tradingProfiles.getRecord(profileId)

        val executionId = when (formType) {
            is Edit -> tradingRecord.executions.edit(
                id = formType.id,
                broker = "Finvasia",
                instrument = formModel.instrument.value!!,
                ticker = formModel.ticker.value!!,
                quantity = formModel.quantity.value.toBigDecimal(),
                lots = formModel.lots.value.ifBlank { null }?.toInt(),
                side = if (formModel.isBuy.value) TradeExecutionSide.Buy else TradeExecutionSide.Sell,
                price = formModel.price.value.toBigDecimal(),
                timestamp = formModel.timestamp.value,
            )

            else -> tradingRecord.executions.new(
                broker = "Finvasia",
                instrument = formModel.instrument.value!!,
                ticker = formModel.ticker.value!!,
                quantity = formModel.quantity.value.toBigDecimal(),
                lots = formModel.lots.value.ifBlank { null }?.toInt(),
                side = if (formModel.isBuy.value) TradeExecutionSide.Buy else TradeExecutionSide.Sell,
                price = formModel.price.value.toBigDecimal(),
                timestamp = formModel.timestamp.value,
                locked = false,
            )
        }

        // Notify execution saved
        onExecutionSaved?.invoke(executionId)
    }

    private fun new(formModel: ((FormValidator) -> TradeExecutionFormModel)? = null) {

        this.formModel = formModel?.invoke(formValidator) ?: TradeExecutionFormModel(
            validator = formValidator,
            instrument = null,
            ticker = null,
            quantity = "",
            lots = "",
            isBuy = true,
            price = "",
            timestamp = run {
                val currentTime = Clock.System.now()
                val currentTimeWithoutNanoseconds = currentTime - currentTime.nanosecondsOfSecond.nanoseconds
                currentTimeWithoutNanoseconds.toLocalDateTime(TimeZone.currentSystemDefault())
            },
        )
    }

    private fun newFromExisting(id: Long) = coroutineScope.launchUnit {

        val tradingRecord = tradingProfiles.getRecord(profileId)

        val execution = tradingRecord.executions.getById(id).first()

        formModel = TradeExecutionFormModel(
            validator = formValidator,
            instrument = execution.instrument,
            ticker = execution.ticker,
            quantity = execution.quantity.toString(),
            lots = execution.lots?.toString() ?: "",
            isBuy = execution.side == TradeExecutionSide.Buy,
            price = execution.price.toPlainString(),
            timestamp = run {
                val currentTime = Clock.System.now()
                val currentTimeWithoutNanoseconds = currentTime - currentTime.nanosecondsOfSecond.nanoseconds
                currentTimeWithoutNanoseconds.toLocalDateTime(TimeZone.currentSystemDefault())
            },
        )
    }

    private fun edit(id: Long) = coroutineScope.launchUnit {

        val tradingRecord = tradingProfiles.getRecord(profileId)

        val execution = tradingRecord.executions.getById(id).first()

        formModel = TradeExecutionFormModel(
            validator = formValidator,
            instrument = execution.instrument,
            ticker = execution.ticker,
            quantity = execution.quantity.toString(),
            lots = execution.lots?.toString() ?: "",
            isBuy = execution.side == TradeExecutionSide.Buy,
            price = execution.price.toPlainString(),
            timestamp = execution.timestamp,
        )
    }
}
