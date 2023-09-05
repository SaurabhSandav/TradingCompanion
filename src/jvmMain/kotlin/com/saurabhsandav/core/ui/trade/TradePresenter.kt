package com.saurabhsandav.core.ui.trade

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.common.app.AppWindowsManager
import com.saurabhsandav.core.ui.landing.model.LandingState.TradeExecutionFormWindowParams
import com.saurabhsandav.core.ui.trade.model.TradeEvent
import com.saurabhsandav.core.ui.trade.model.TradeEvent.*
import com.saurabhsandav.core.ui.trade.model.TradeState
import com.saurabhsandav.core.ui.trade.model.TradeState.*
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType
import com.saurabhsandav.core.utils.launchUnit
import com.saurabhsandav.core.utils.mapList
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import java.time.format.DateTimeFormatter
import java.util.*

@Stable
internal class TradePresenter(
    private val profileId: Long,
    private val tradeId: Long,
    private val coroutineScope: CoroutineScope,
    private val appModule: AppModule,
    private val executionFormWindowsManager: AppWindowsManager<TradeExecutionFormWindowParams>,
    private val tradingProfiles: TradingProfiles = appModule.tradingProfiles,
) {

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        val tradingProfileName by remember {
            tradingProfiles.getProfile(profileId).map { profile -> "${profile.name} - " }
        }.collectAsState("")

        return@launchMolecule TradeState(
            title = "${tradingProfileName}Trade ($tradeId)",
            details = getTradeDetail().value,
            executions = getTradeExecutions().value,
            stops = getTradeStops().value,
            targets = getTradeTargets().value,
            mfeAndMae = getMfeAndMae().value,
            notes = getTradeNotes().value,
            eventSink = ::onEvent,
        )
    }

    val errors = mutableStateListOf<UIErrorMessage>()

    private fun onEvent(event: TradeEvent) {

        when (event) {
            is EditExecution -> onEditExecution(event.executionId)
            is LockExecution -> onLockExecution(event.executionId)
            is DeleteExecution -> onDeleteExecution(event.executionId)
            is AddStop -> onAddStop(event.price)
            is DeleteStop -> onDeleteStop(event.price)
            is AddTarget -> onAddTarget(event.price)
            is DeleteTarget -> onDeleteTarget(event.price)
            is AddNote -> onAddNote(event.note)
            is UpdateNote -> onUpdateNote(event.id, event.note)
            is DeleteNote -> onDeleteNote(event.id)
        }
    }

    @Composable
    private fun getTradeDetail(): State<Details?> {
        return produceState<Details?>(null) {

            val tradingRecord = tradingProfiles.getRecord(profileId)

            tradingRecord.trades.getById(tradeId).collect { trade ->

                val instrumentCapitalized = trade.instrument.strValue
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

                val timeZone = TimeZone.of("Asia/Kolkata")
                val entryInstant = trade.entryTimestamp.toInstant(timeZone)
                val exitInstant = trade.exitTimestamp?.toInstant(timeZone)
                val s = exitInstant?.let { (it - entryInstant).inWholeSeconds }

                val duration = s?.let { "%02d:%02d:%02d".format(it / 3600, (it % 3600) / 60, (it % 60)) }

                value = Details(
                    id = trade.id,
                    broker = "${trade.broker} ($instrumentCapitalized)",
                    ticker = trade.ticker,
                    side = trade.side.toString().uppercase(),
                    quantity = (trade.lots?.let { "${trade.closedQuantity} / ${trade.quantity} ($it ${if (it == 1) "lot" else "lots"})" }
                        ?: "${trade.closedQuantity} / ${trade.quantity}").toString(),
                    entry = trade.averageEntry.toPlainString(),
                    exit = trade.averageExit?.toPlainString() ?: "",
                    duration = "${trade.entryTimestamp.time} -> ${trade.exitTimestamp?.time ?: "Now"} ${duration?.let { "($it)" }}",
                    pnl = trade.pnl.toPlainString(),
                    isProfitable = trade.pnl > BigDecimal.ZERO,
                    netPnl = trade.netPnl.toPlainString(),
                    isNetProfitable = trade.netPnl > BigDecimal.ZERO,
                    fees = trade.fees.toPlainString(),
                )
            }
        }
    }

    @Composable
    private fun getTradeExecutions(): State<ImmutableList<Execution>> {
        return produceState<ImmutableList<Execution>>(persistentListOf()) {

            val tradingRecord = tradingProfiles.getRecord(profileId)

            tradingRecord.trades.getExecutionsForTrade(tradeId).collect { executions ->

                value = executions.map { execution ->

                    Execution(
                        id = execution.id,
                        quantity = execution.lots
                            ?.let { "${execution.quantity} ($it ${if (it == 1) "lot" else "lots"})" }
                            ?: execution.quantity.toString(),
                        side = execution.side.strValue.uppercase(),
                        price = execution.price.toPlainString(),
                        timestamp = execution.timestamp.time.toString(),
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

            tradingRecord.trades.getStopsForTrade(tradeId).collect { stops ->

                value = stops.map { stop ->
                    TradeStop(
                        price = stop.price,
                        priceText = stop.price.toPlainString(),
                        risk = stop.risk.toPlainString(),
                    )
                }.toImmutableList()
            }
        }
    }

    @Composable
    private fun getTradeTargets(): State<ImmutableList<TradeTarget>> {
        return produceState<ImmutableList<TradeTarget>>(persistentListOf()) {

            val tradingRecord = tradingProfiles.getRecord(profileId)

            tradingRecord.trades.getTargetsForTrade(tradeId).collect { targets ->

                value = targets.map { target ->
                    TradeTarget(
                        price = target.price,
                        priceText = target.price.toPlainString(),
                        profit = target.profit.toPlainString(),
                    )
                }.toImmutableList()
            }
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

    private fun onEditExecution(executionId: Long) {

        val window = executionFormWindowsManager.windows.find {
            it.params.formType is TradeExecutionFormType.Edit && it.params.formType.id == executionId
        }

        when (window) {
            // Open new window
            null -> {

                val params = TradeExecutionFormWindowParams(
                    profileId = profileId,
                    formType = TradeExecutionFormType.Edit(executionId),
                )

                executionFormWindowsManager.newWindow(params)
            }

            // Window already open. Bring to front.
            else -> window.toFront()
        }
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
