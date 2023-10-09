package com.saurabhsandav.core.ui.trade

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.TradeSide
import com.saurabhsandav.core.ui.TradeContentLauncher
import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.trade.model.TradeEvent
import com.saurabhsandav.core.ui.trade.model.TradeEvent.*
import com.saurabhsandav.core.ui.trade.model.TradeState
import com.saurabhsandav.core.ui.trade.model.TradeState.*
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType
import com.saurabhsandav.core.utils.brokerage
import com.saurabhsandav.core.utils.launchUnit
import com.saurabhsandav.core.utils.mapList
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import java.math.BigDecimal
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Stable
internal class TradePresenter(
    private val profileId: Long,
    private val tradeId: Long,
    private val coroutineScope: CoroutineScope,
    private val appModule: AppModule,
    private val tradeContentLauncher: TradeContentLauncher,
    private val tradingProfiles: TradingProfiles = appModule.tradingProfiles,
) {

    private val trade = flow { emitAll(tradingProfiles.getRecord(profileId).trades.getById(tradeId)) }.shareIn(
        scope = coroutineScope,
        started = SharingStarted.Eagerly,
        replay = 1,
    )

    private var newExecutionEnabled by mutableStateOf(false)

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        val tradingProfileName by remember {
            tradingProfiles.getProfile(profileId).map { profile -> "${profile.name} - " }
        }.collectAsState("")

        return@launchMolecule TradeState(
            title = "${tradingProfileName}Trade ($tradeId)",
            details = getTradeDetail().value,
            executions = getTradeExecutions().value,
            newExecutionEnabled = newExecutionEnabled,
            stops = getTradeStops().value,
            previewStop = ::previewStop,
            targets = getTradeTargets().value,
            previewTarget = ::previewTarget,
            mfeAndMae = getMfeAndMae().value,
            tags = getTradeTags().value,
            tagSuggestions = ::tagSuggestions,
            notes = getTradeNotes().value,
            eventSink = ::onEvent,
        )
    }

    val errors = mutableStateListOf<UIErrorMessage>()

    private fun onEvent(event: TradeEvent) {

        when (event) {
            is AddToTrade -> onAddToTrade()
            is CloseTrade -> onCloseTrade()
            is NewFromExistingExecution -> onNewExecution(event.fromExecutionId)
            is EditExecution -> onEditExecution(event.executionId)
            is LockExecution -> onLockExecution(event.executionId)
            is DeleteExecution -> onDeleteExecution(event.executionId)
            is AddStop -> onAddStop(event.price)
            is DeleteStop -> onDeleteStop(event.price)
            is AddTarget -> onAddTarget(event.price)
            is DeleteTarget -> onDeleteTarget(event.price)
            is AddTag -> onAddTag(event.id)
            is RemoveTag -> onRemoveTag(event.id)
            is AddNote -> onAddNote(event.note)
            is UpdateNote -> onUpdateNote(event.id, event.note)
            is DeleteNote -> onDeleteNote(event.id)
        }
    }

    @Composable
    private fun getTradeDetail(): State<Details?> {
        return produceState<Details?>(null) {

            trade.collect { trade ->

                val instrumentCapitalized = trade.instrument.strValue
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

                val timeZone = TimeZone.of("Asia/Kolkata")
                val entryInstant = trade.entryTimestamp.toInstant(timeZone)
                val exitInstant = trade.exitTimestamp?.toInstant(timeZone)

                fun formatDuration(duration: Duration): String {

                    val durationSeconds = duration.inWholeSeconds

                    return "%02d:%02d:%02d".format(
                        durationSeconds / 3600,
                        (durationSeconds % 3600) / 60,
                        durationSeconds % 60,
                    )
                }

                val durationStr = when {
                    trade.isClosed -> flowOf(formatDuration(exitInstant!! - entryInstant))
                    else -> flow {
                        while (true) {
                            emit(formatDuration(Clock.System.now() - entryInstant))
                            delay(1.seconds)
                        }
                    }
                }

                value = Details(
                    id = trade.id,
                    broker = "${trade.broker} ($instrumentCapitalized)",
                    ticker = trade.ticker,
                    side = trade.side.toString().uppercase(),
                    quantity = when {
                        !trade.isClosed -> "${trade.closedQuantity} / ${trade.quantity}"
                        else -> trade.quantity.toPlainString()
                    },
                    entry = trade.averageEntry.toPlainString(),
                    exit = trade.averageExit?.toPlainString() ?: "",
                    duration = durationStr,
                    pnl = trade.pnl.toPlainString(),
                    isProfitable = trade.pnl > BigDecimal.ZERO,
                    netPnl = trade.netPnl.toPlainString(),
                    isNetProfitable = trade.netPnl > BigDecimal.ZERO,
                    fees = trade.fees.toPlainString(),
                )

                newExecutionEnabled = !trade.isClosed
            }
        }
    }

    @Composable
    private fun getTradeExecutions(): State<ImmutableList<Execution>> {
        return produceState<ImmutableList<Execution>>(persistentListOf()) {

            val tradingRecord = tradingProfiles.getRecord(profileId)

            val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy - HH:mm:ss")

            tradingRecord.trades.getExecutionsForTrade(tradeId).collect { executions ->

                value = executions.map { execution ->

                    Execution(
                        id = execution.id,
                        quantity = execution.lots
                            ?.let { "${execution.quantity} ($it ${if (it == 1) "lot" else "lots"})" }
                            ?: execution.quantity.toString(),
                        side = execution.side.strValue.uppercase(),
                        price = execution.price.toPlainString(),
                        timestamp = formatter.format(execution.timestamp.toJavaLocalDateTime()),
                        locked = execution.locked,
                    )
                }.toImmutableList()
            }
        }
    }

    @Composable
    private fun getTradeStops(): State<ImmutableList<TradeStop>> {
        return produceState<ImmutableList<TradeStop>>(persistentListOf()) {

            val tradingRecord = tradingProfiles.getRecord(profileId)

            trade.combine(tradingRecord.trades.getStopsForTrade(tradeId)) { trade, stops ->

                stops.map { stop ->

                    val brokerage = brokerage(
                        broker = trade.broker,
                        instrument = trade.instrument,
                        entry = trade.averageEntry,
                        exit = stop.price,
                        quantity = trade.quantity,
                        side = trade.side,
                    )

                    TradeStop(
                        price = stop.price,
                        priceText = stop.price.toPlainString(),
                        risk = brokerage.pnl.toPlainString(),
                        netRisk = brokerage.netPNL.toPlainString(),
                    )
                }.toImmutableList()
            }.collect { tradeStops -> value = tradeStops }
        }
    }

    @Composable
    private fun getTradeTargets(): State<ImmutableList<TradeTarget>> {
        return produceState<ImmutableList<TradeTarget>>(persistentListOf()) {

            val tradingRecord = tradingProfiles.getRecord(profileId)

            trade.combine(tradingRecord.trades.getTargetsForTrade(tradeId)) { trade, targets ->

                targets.map { target ->

                    val brokerage = brokerage(
                        broker = trade.broker,
                        instrument = trade.instrument,
                        entry = trade.averageEntry,
                        exit = target.price,
                        quantity = trade.quantity,
                        side = trade.side,
                    )

                    TradeTarget(
                        price = target.price,
                        priceText = target.price.toPlainString(),
                        profit = brokerage.pnl.toPlainString(),
                        netProfit = brokerage.netPNL.toPlainString(),
                    )
                }.toImmutableList()
            }.collect { tradeTargets -> value = tradeTargets }
        }
    }

    @Composable
    private fun getMfeAndMae(): State<MfeAndMae?> {
        return produceState<MfeAndMae?>(null) {

            val tradingRecord = tradingProfiles.getRecord(profileId)

            tradingRecord.trades.getMfeAndMae(tradeId).map { mfeAndMae ->

                mfeAndMae ?: return@map null

                MfeAndMae(
                    mfePrice = mfeAndMae.mfePrice.toPlainString(),
                    maePrice = mfeAndMae.maePrice.toPlainString(),
                )
            }
        }
    }

    @Composable
    private fun getTradeTags(): State<ImmutableList<TradeTag>> {
        return produceState<ImmutableList<TradeTag>>(persistentListOf()) {

            val tradingRecord = tradingProfiles.getRecord(profileId)

            tradingRecord.trades
                .getTagsForTrade(tradeId)
                .mapList { tag ->

                    TradeTag(
                        id = tag.id,
                        name = tag.name,
                        description = tag.description,
                    )
                }
                .collect { value = it.toImmutableList() }
        }
    }

    @Composable
    private fun getTradeNotes(): State<ImmutableList<TradeNote>> {
        return produceState<ImmutableList<TradeNote>>(persistentListOf()) {

            val tradingRecord = tradingProfiles.getRecord(profileId)

            tradingRecord.trades.getNotesForTrade(tradeId)
                .mapList { note ->

                    val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy hh:mm:ss")

                    val added = note.added
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .toJavaLocalDateTime()
                        .let(formatter::format)

                    val lastEdited = note.lastEdited
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .toJavaLocalDateTime()
                        .let(formatter::format)

                    TradeNote(
                        id = note.id,
                        note = note.note,
                        dateText = "Added $added (Last Edited $lastEdited)",
                    )
                }
                .collect { value = it.toImmutableList() }
        }
    }

    private fun previewStop(price: BigDecimal): Flow<TradeStop?> {
        return trade.map { trade ->

            val stopIsValid = when (trade.side) {
                TradeSide.Long -> price < trade.averageEntry
                TradeSide.Short -> price > trade.averageEntry
            }

            when {
                !stopIsValid -> null
                else -> {

                    val brokerage = brokerage(
                        broker = trade.broker,
                        instrument = trade.instrument,
                        entry = trade.averageEntry,
                        exit = price,
                        quantity = trade.quantity,
                        side = trade.side,
                    )

                    TradeStop(
                        price = price,
                        priceText = price.toPlainString(),
                        risk = brokerage.pnl.toPlainString(),
                        netRisk = brokerage.netPNL.toPlainString(),
                    )
                }
            }
        }
    }

    private fun previewTarget(price: BigDecimal): Flow<TradeTarget?> {
        return trade.map { trade ->

            val targetIsValid = when (trade.side) {
                TradeSide.Long -> price > trade.averageEntry
                TradeSide.Short -> price < trade.averageEntry
            }

            when {
                !targetIsValid -> null
                else -> {

                    val brokerage = brokerage(
                        broker = trade.broker,
                        instrument = trade.instrument,
                        entry = trade.averageEntry,
                        exit = price,
                        quantity = trade.quantity,
                        side = trade.side,
                    )

                    TradeTarget(
                        price = price,
                        priceText = price.toPlainString(),
                        profit = brokerage.pnl.toPlainString(),
                        netProfit = brokerage.netPNL.toPlainString(),
                    )
                }
            }
        }
    }

    private fun tagSuggestions(filter: String): Flow<ImmutableList<TradeTag>> = flow {

        val tradingRecord = tradingProfiles.getRecord(profileId)

        tradingRecord.trades
            .getSuggestedTagsForTrade(tradeId, filter)
            .mapList { tag ->

                TradeTag(
                    id = tag.id,
                    name = tag.name,
                    description = tag.description,
                )
            }
            .map { it.toImmutableList() }
            .let { emitAll(it) }
    }

    private fun onAddToTrade() {

        tradeContentLauncher.openExecutionForm(
            profileId = profileId,
            formType = TradeExecutionFormType.AddToTrade(tradeId),
        )
    }

    private fun onCloseTrade() {

        tradeContentLauncher.openExecutionForm(
            profileId = profileId,
            formType = TradeExecutionFormType.CloseTrade(tradeId),
        )
    }

    private fun onNewExecution(fromExecutionId: Long) {

        tradeContentLauncher.openExecutionForm(
            profileId = profileId,
            formType = TradeExecutionFormType.NewFromExistingInTrade(fromExecutionId),
        )
    }

    private fun onEditExecution(executionId: Long) {

        tradeContentLauncher.openExecutionForm(
            profileId = profileId,
            formType = TradeExecutionFormType.Edit(executionId),
        )
    }

    private fun onLockExecution(executionId: Long) = coroutineScope.launchUnit {

        val tradingRecord = tradingProfiles.getRecord(profileId)

        tradingRecord.executions.lock(listOf(executionId))
    }

    private fun onDeleteExecution(executionId: Long) = coroutineScope.launchUnit {

        val tradingRecord = tradingProfiles.getRecord(profileId)

        tradingRecord.executions.delete(listOf(executionId))
    }

    private fun onAddStop(price: BigDecimal) = coroutineScope.launchUnit {

        tradingProfiles.getRecord(profileId).trades.addStop(tradeId, price)
    }

    private fun onDeleteStop(price: BigDecimal) = coroutineScope.launchUnit {

        tradingProfiles.getRecord(profileId).trades.deleteStop(tradeId, price)
    }

    private fun onAddTarget(price: BigDecimal) = coroutineScope.launchUnit {

        tradingProfiles.getRecord(profileId).trades.addTarget(tradeId, price)
    }

    private fun onDeleteTarget(price: BigDecimal) = coroutineScope.launchUnit {

        tradingProfiles.getRecord(profileId).trades.deleteTarget(tradeId, price)
    }

    private fun onAddTag(id: Long) = coroutineScope.launchUnit {

        tradingProfiles.getRecord(profileId).trades.addTag(tradeId, id)
    }

    private fun onRemoveTag(id: Long) = coroutineScope.launchUnit {

        tradingProfiles.getRecord(profileId).trades.removeTag(tradeId, id)
    }

    private fun onAddNote(note: String) = coroutineScope.launchUnit {

        tradingProfiles.getRecord(profileId).trades.addNote(tradeId, note)
    }

    private fun onUpdateNote(id: Long, note: String) = coroutineScope.launchUnit {

        tradingProfiles.getRecord(profileId).trades.updateNote(id, note)
    }

    private fun onDeleteNote(id: Long) = coroutineScope.launchUnit {

        tradingProfiles.getRecord(profileId).trades.deleteNote(id)
    }
}
