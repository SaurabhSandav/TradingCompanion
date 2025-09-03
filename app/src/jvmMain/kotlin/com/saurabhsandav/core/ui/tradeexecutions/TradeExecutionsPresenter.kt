package com.saurabhsandav.core.ui.tradeexecutions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.insertSeparators
import androidx.paging.map
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.trading.ProfileId
import com.saurabhsandav.core.trading.TradingProfiles
import com.saurabhsandav.core.ui.common.SelectionManager
import com.saurabhsandav.core.ui.common.TradeDateTimeFormat
import com.saurabhsandav.core.ui.tradecontent.TradeContentLauncher
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsEvent
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsEvent.DeleteExecutions
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsEvent.EditExecution
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsEvent.LockExecutions
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsEvent.NewExecution
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsEvent.NewExecutionFromExisting
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsState
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsState.TradeExecutionEntry
import com.saurabhsandav.core.utils.emitInto
import com.saurabhsandav.core.utils.launchUnit
import com.saurabhsandav.trading.record.TradeExecutionDisplay
import com.saurabhsandav.trading.record.model.TradeExecutionId
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import java.util.Locale
import kotlin.time.Clock

@AssistedInject
internal class TradeExecutionsPresenter(
    @Assisted private val coroutineScope: CoroutineScope,
    private val profileId: ProfileId,
    private val tradeContentLauncher: TradeContentLauncher,
    private val tradingProfiles: TradingProfiles,
) {

    private val executions = coroutineScope.async { tradingProfiles.getRecord(profileId).executions }
    private val selectionManager = SelectionManager<TradeExecutionId>()

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule TradeExecutionsState(
            executionEntries = getExecutionEntries(),
            selectionManager = selectionManager,
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
                pagingSourceFactory = executions::getAllDisplayPagingSource,
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
                                (before == null || before.timestamp >= startOfToday) &&
                                    after.timestamp < startOfToday -> TradeExecutionEntry.Section(
                                    isToday = false,
                                    count = executions.getBeforeTodayCount(),
                                )

                                else -> null
                            }
                        }
                        .map { executionOrEntry ->
                            when (executionOrEntry) {
                                is TradeExecutionDisplay -> executionOrEntry.toTradeExecutionEntryItem()
                                else -> executionOrEntry
                            }
                        } as PagingData<TradeExecutionEntry>
                }
                .emitInto(this)
        }
    }

    private fun TradeExecutionDisplay.toTradeExecutionEntryItem() = TradeExecutionEntry.Item(
        id = id,
        broker = run {
            val instrumentCapitalized = instrument.strValue
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            "$brokerName ($instrumentCapitalized)"
        },
        ticker = ticker,
        quantity = lots?.let { "$quantity ($it ${if (it == 1) "lot" else "lots"})" } ?: quantity.toString(),
        side = side.strValue.uppercase(),
        price = price.toString(),
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

    @AssistedFactory
    fun interface Factory {

        fun create(coroutineScope: CoroutineScope): TradeExecutionsPresenter
    }
}
