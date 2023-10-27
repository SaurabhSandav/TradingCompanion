package com.saurabhsandav.core.ui.tradeexecutions

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trades.TradeExecution
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.ui.TradeContentLauncher
import com.saurabhsandav.core.ui.common.SelectionManager
import com.saurabhsandav.core.ui.common.TradeDateTimeFormatter
import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.common.format
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsEvent
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsEvent.*
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsState
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsState.ProfileTradeExecutionId
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsState.TradeExecutionEntry
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.util.*

@Stable
internal class TradeExecutionsPresenter(
    private val coroutineScope: CoroutineScope,
    private val appModule: AppModule,
    private val tradeContentLauncher: TradeContentLauncher,
    private val tradingProfiles: TradingProfiles = appModule.tradingProfiles,
) {

    private val errors = mutableStateListOf<UIErrorMessage>()
    private val selectionManager = SelectionManager<TradeExecutionEntry>()

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule TradeExecutionsState(
            executions = getExecutions().value,
            selectionManager = selectionManager,
            errors = remember(errors) { errors.toImmutableList() },
            eventSink = ::onEvent,
        )
    }

    private fun onEvent(event: TradeExecutionsEvent) {

        when (event) {
            NewExecution -> onNewExecution()
            is NewExecutionFromExisting -> onNewExecutionFromExisting(event.profileTradeExecutionId)
            is EditExecution -> onEditExecution(event.profileTradeExecutionId)
            is LockExecutions -> onLockExecutions(event.ids)
            is DeleteExecutions -> onDeleteExecutions(event.ids)
        }
    }

    @Composable
    private fun getExecutions(): State<ImmutableList<TradeExecutionEntry>> {
        return remember {
            tradingProfiles.currentProfile.flatMapLatest { profile ->

                // Clear execution selection
                selectionManager.clear()

                val tradingRecord = tradingProfiles.getRecord(profile.id)

                tradingRecord.executions.allExecutions.map { executions ->
                    executions
                        .map { it.toTradeExecutionListEntry(profile.id) }
                        .toImmutableList()
                }
            }
        }.collectAsState(persistentListOf())
    }

    private fun TradeExecution.toTradeExecutionListEntry(profileId: Long) = TradeExecutionEntry(
        profileTradeExecutionId = ProfileTradeExecutionId(profileId = profileId, executionId = id),
        broker = run {
            val instrumentCapitalized = instrument.strValue
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            "$broker ($instrumentCapitalized)"
        },
        ticker = ticker,
        quantity = lots?.let { "$quantity ($it ${if (it == 1) "lot" else "lots"})" } ?: quantity.toString(),
        side = side.strValue.uppercase(),
        price = price.toPlainString(),
        timestamp = TradeDateTimeFormatter.format(timestamp),
        locked = locked,
    )

    private fun onNewExecution() = coroutineScope.launchUnit {

        val currentProfile = tradingProfiles.currentProfile.first()

        tradeContentLauncher.openExecutionForm(
            profileId = currentProfile.id,
            formType = TradeExecutionFormType.New,
        )
    }

    private fun onNewExecutionFromExisting(profileTradeExecutionId: ProfileTradeExecutionId) {

        tradeContentLauncher.openExecutionForm(
            profileId = profileTradeExecutionId.profileId,
            formType = TradeExecutionFormType.NewFromExisting(profileTradeExecutionId.executionId),
        )
    }

    private fun onEditExecution(profileTradeExecutionId: ProfileTradeExecutionId) {

        tradeContentLauncher.openExecutionForm(
            profileId = profileTradeExecutionId.profileId,
            formType = TradeExecutionFormType.Edit(profileTradeExecutionId.executionId),
        )
    }

    private fun onLockExecutions(ids: List<ProfileTradeExecutionId>) = coroutineScope.launchUnit {

        ids.groupBy { it.profileId }.forEach { (profileId, ids) ->

            val tradingRecord = tradingProfiles.getRecord(profileId)

            tradingRecord.executions.lock(ids.map { it.executionId })
        }
    }

    private fun onDeleteExecutions(ids: List<ProfileTradeExecutionId>) = coroutineScope.launchUnit {

        ids.groupBy { it.profileId }.forEach { (profileId, ids) ->

            val tradingRecord = tradingProfiles.getRecord(profileId)

            tradingRecord.executions.delete(ids.map { it.executionId })
        }
    }
}
