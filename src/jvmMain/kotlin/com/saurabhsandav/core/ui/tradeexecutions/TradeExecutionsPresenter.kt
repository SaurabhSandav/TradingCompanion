package com.saurabhsandav.core.ui.tradeexecutions

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.trades.TradeExecution
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.model.TradeExecutionId
import com.saurabhsandav.core.ui.common.SelectionManager
import com.saurabhsandav.core.ui.common.TradeDateTimeFormatter
import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.tradecontent.TradeContentLauncher
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsEvent
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsEvent.*
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsState
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsState.TradeExecutionEntry
import com.saurabhsandav.core.utils.emitInto
import com.saurabhsandav.core.utils.format
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.*

@Stable
internal class TradeExecutionsPresenter(
    private val coroutineScope: CoroutineScope,
    private val profileId: ProfileId,
    private val tradeContentLauncher: TradeContentLauncher,
    private val tradingProfiles: TradingProfiles,
) {

    private val errors = mutableStateListOf<UIErrorMessage>()
    private val selectionManager = SelectionManager<TradeExecutionId>()
    private var canSelectionLock by mutableStateOf(false)

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule TradeExecutionsState(
            todayExecutions = getTodayExecutions().value,
            pastExecutions = getPastExecutions().value,
            selectionManager = selectionManager,
            canSelectionLock = canSelectionLock,
            errors = remember(errors) { errors.toImmutableList() },
            eventSink = ::onEvent,
        )
    }

    private fun onEvent(event: TradeExecutionsEvent) {

        when (event) {
            NewExecution -> onNewExecution()
            is NewExecutionFromExisting -> onNewExecutionFromExisting(event.id)
            is EditExecution -> onEditExecution(event.id)
            is LockExecutions -> onLockExecutions(event.ids)
            is DeleteExecutions -> onDeleteExecutions(event.ids)
        }
    }

    init {

        coroutineScope.launch {

            val executionsRepo = tradingProfiles.getRecord(profileId).executions

            snapshotFlow { selectionManager.selection.toList() }
                .flatMapLatest(executionsRepo::getByIds)
                .collect { executions ->
                    canSelectionLock = executions.any { !it.locked }
                }
        }
    }

    @Composable
    private fun getTodayExecutions(): State<ImmutableList<TradeExecutionEntry>> {
        return remember {

            flow {

                tradingProfiles
                    .getRecord(profileId)
                    .executions
                    .getToday()
                    .map { executions ->
                        executions
                            .map { it.toTradeExecutionListEntry() }
                            .toImmutableList()
                    }
                    .emitInto(this)
            }
        }.collectAsState(persistentListOf())
    }

    @Composable
    private fun getPastExecutions(): State<ImmutableList<TradeExecutionEntry>> {
        return remember {

            flow {

                tradingProfiles
                    .getRecord(profileId)
                    .executions
                    .getBeforeToday()
                    .map { executions ->
                        executions
                            .map { it.toTradeExecutionListEntry() }
                            .toImmutableList()
                    }
                    .emitInto(this)
            }
        }.collectAsState(persistentListOf())
    }

    private fun TradeExecution.toTradeExecutionListEntry() = TradeExecutionEntry(
        id = id,
        broker = run {
            val instrumentCapitalized = instrument.strValue
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            "$broker ($instrumentCapitalized)"
        },
        ticker = ticker,
        quantity = lots?.let { "$quantity ($it ${if (it == 1) "lot" else "lots"})" } ?: quantity.toString(),
        side = side.strValue.uppercase(),
        price = price.toPlainString(),
        timestamp = TradeDateTimeFormatter.format(timestamp.toLocalDateTime(TimeZone.currentSystemDefault())),
        locked = locked,
    )

    private fun onNewExecution() {

        tradeContentLauncher.openExecutionForm(
            profileId = profileId,
            formType = TradeExecutionFormType.New,
        )
    }

    private fun onNewExecutionFromExisting(id: TradeExecutionId) {

        tradeContentLauncher.openExecutionForm(
            profileId = profileId,
            formType = TradeExecutionFormType.NewFromExisting(id),
        )
    }

    private fun onEditExecution(id: TradeExecutionId) {

        tradeContentLauncher.openExecutionForm(
            profileId = profileId,
            formType = TradeExecutionFormType.Edit(id),
        )
    }

    private fun onLockExecutions(ids: List<TradeExecutionId>) = coroutineScope.launchUnit {

        val tradingRecord = tradingProfiles.getRecord(profileId)

        tradingRecord.executions.lock(ids)
    }

    private fun onDeleteExecutions(ids: List<TradeExecutionId>) = coroutineScope.launchUnit {

        val tradingRecord = tradingProfiles.getRecord(profileId)

        tradingRecord.executions.delete(ids)
    }
}
