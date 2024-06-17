package com.saurabhsandav.core.ui.tradeexecutions

import androidx.compose.runtime.*
import androidx.paging.*
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.trades.TradeExecution
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.model.TradeExecutionId
import com.saurabhsandav.core.ui.common.SelectionManager
import com.saurabhsandav.core.ui.common.TradeDateTimeFormat
import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.tradecontent.TradeContentLauncher
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsEvent
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsEvent.*
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsState
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsState.TradeExecutionEntry
import com.saurabhsandav.core.utils.emitInto
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import java.util.*

internal class TradeExecutionsPresenter(
    private val coroutineScope: CoroutineScope,
    private val profileId: ProfileId,
    private val tradeContentLauncher: TradeContentLauncher,
    private val tradingProfiles: TradingProfiles,
) {

    private val executions = coroutineScope.async { tradingProfiles.getRecord(profileId).executions }
    private val errors = mutableStateListOf<UIErrorMessage>()
    private val selectionManager = SelectionManager<TradeExecutionId>()
    private var canSelectionLock by mutableStateOf(false)

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule TradeExecutionsState(
            executionEntries = getExecutionEntries(),
            selectionManager = selectionManager,
            canSelectionLock = canSelectionLock,
            errors = errors,
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

            snapshotFlow { selectionManager.selection.toList() }
                .flatMapLatest { executions.await().getByIds(it) }
                .collect { executions ->
                    canSelectionLock = executions.any { !it.locked }
                }
        }
    }

    @Composable
    private fun getExecutionEntries(): Flow<PagingData<TradeExecutionEntry>> = remember {
        flow {

            val executions = executions.await()

            val pager = Pager(
                config = PagingConfig(
                    pageSize = 70,
                    enablePlaceholders = false,
                    maxSize = 300,
                ),
                pagingSourceFactory = executions::getAllPagingSource,
            )

            pager
                .flow
                .map { pagingData ->

                    val tz = TimeZone.currentSystemDefault()
                    val startOfToday = Clock.System.now().toLocalDateTime(tz).date.atStartOfDayIn(tz)

                    @Suppress("UNCHECKED_CAST")
                    pagingData
                        .insertSeparators { before, after ->

                            when {
                                // If before is the last execution
                                after == null -> null

                                // If first execution is from today
                                before == null && after.timestamp >= startOfToday -> TradeExecutionEntry.Section(
                                    isToday = true,
                                    count = executions.getTodayCount(),
                                )

                                // If either after is first execution or before is from today
                                // And after is from before today
                                (before == null || before.timestamp >= startOfToday)
                                        && after.timestamp < startOfToday -> TradeExecutionEntry.Section(
                                    isToday = false,
                                    count = executions.getBeforeTodayCount(),
                                )

                                else -> null
                            }
                        }
                        .map { executionOrEntry ->
                            when (executionOrEntry) {
                                is TradeExecution -> executionOrEntry.toTradeExecutionEntryItem()
                                else -> executionOrEntry
                            }
                        } as PagingData<TradeExecutionEntry>
                }
                .emitInto(this)
        }
    }

    private fun TradeExecution.toTradeExecutionEntryItem() = TradeExecutionEntry.Item(
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
        timestamp = timestamp.toLocalDateTime(TimeZone.currentSystemDefault()).format(TradeDateTimeFormat),
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

        executions.await().lock(ids)
    }

    private fun onDeleteExecutions(ids: List<TradeExecutionId>) = coroutineScope.launchUnit {

        executions.await().delete(ids)
    }
}
