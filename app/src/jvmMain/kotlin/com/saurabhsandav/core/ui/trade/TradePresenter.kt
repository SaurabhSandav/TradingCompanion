package com.saurabhsandav.core.ui.trade

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.trades.*
import com.saurabhsandav.core.trades.model.*
import com.saurabhsandav.core.ui.common.TradeDateTimeFormat
import com.saurabhsandav.core.ui.tags.model.TradeTag
import com.saurabhsandav.core.ui.trade.model.TradeEvent
import com.saurabhsandav.core.ui.trade.model.TradeEvent.*
import com.saurabhsandav.core.ui.trade.model.TradeState
import com.saurabhsandav.core.ui.trade.model.TradeState.*
import com.saurabhsandav.core.ui.trade.model.TradeState.Excursions
import com.saurabhsandav.core.ui.trade.model.TradeState.TradeAttachment
import com.saurabhsandav.core.ui.trade.model.TradeState.TradeNote
import com.saurabhsandav.core.ui.trade.model.TradeState.TradeStop
import com.saurabhsandav.core.ui.trade.model.TradeState.TradeTarget
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import com.saurabhsandav.core.ui.tradecontent.TradeContentLauncher
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType
import com.saurabhsandav.core.utils.emitInto
import com.saurabhsandav.core.utils.launchUnit
import com.saurabhsandav.core.utils.mapList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import java.math.MathContext
import java.util.*
import kotlin.io.path.extension
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class TradePresenter(
    private val profileTradeId: ProfileTradeId,
    private val onCloseRequest: () -> Unit,
    private val coroutineScope: CoroutineScope,
    private val tradeContentLauncher: TradeContentLauncher,
    private val tradingProfiles: TradingProfiles,
    private val excursionsGenerator: TradeExcursionsGenerator,
) {

    private val profileId = profileTradeId.profileId
    private val tradeId = profileTradeId.tradeId
    private val tradingRecord = coroutineScope.async { tradingProfiles.getRecord(profileId) }

    private val trade = flow {
        tradingRecord.await().trades.getByIdOrNull(tradeId)
            .onEach { if (it == null) onCloseRequest() }
            .filterNotNull()
            .emitInto(this)
    }.shareIn(
        scope = coroutineScope,
        started = SharingStarted.Eagerly,
        replay = 1,
    )
    private val stopPreviewer = trade.map(::StopPreviewer)
    private val targetPreviewer = trade.flatMapLatest { trade ->
        tradingRecord.await().stops.getPrimary(trade.id).map { stop -> TargetPreviewer(trade, stop) }
    }

    private var newExecutionEnabled by mutableStateOf(false)

    init {

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

        return@launchMolecule TradeState(
            title = "${tradingProfileName}Trade ($tradeId)",
            details = getTradeDetail().value,
            executions = getTradeExecutions().value,
            newExecutionEnabled = newExecutionEnabled,
            stops = getTradeStops().value,
            stopPreviewer = stopPreviewer,
            targets = getTradeTargets().value,
            showTargetRValues = showTargetRValues(),
            targetPreviewer = targetPreviewer,
            excursions = getExcursions().value,
            tags = getTradeTags().value,
            attachments = getTradeAttachments().value,
            notes = getTradeNotes().value,
            eventSink = ::onEvent,
        )
    }

    private fun onEvent(event: TradeEvent) {

        when (event) {
            AddToTrade -> onAddToTrade()
            CloseTrade -> onCloseTrade()
            is NewFromExistingExecution -> onNewExecution(event.fromExecutionId)
            is EditExecution -> onEditExecution(event.executionId)
            is LockExecution -> onLockExecution(event.executionId)
            is DeleteExecution -> onDeleteExecution(event.executionId)
            OpenChart -> onOpenChart()
            is AddStop -> onAddStop(event.price)
            is DeleteStop -> onDeleteStop(event.price)
            is SetPrimaryStop -> setPrimaryStop(event.price)
            is AddTarget -> onAddTarget(event.price)
            is DeleteTarget -> onDeleteTarget(event.price)
            is SetPrimaryTarget -> setPrimaryTarget(event.price)
            is AddTag -> onAddTag(event.id)
            is RemoveTag -> onRemoveTag(event.id)
            is RemoveAttachment -> onRemoveAttachment(event.fileId)
            is AddNote -> onAddNote(event.note, event.isMarkdown)
            is UpdateNote -> onUpdateNote(event.id, event.note, event.isMarkdown)
            is DeleteNote -> onDeleteNote(event.id)
        }
    }

    @Composable
    private fun getTradeDetail(): State<Details?> {
        return produceState<Details?>(null) {

            trade.collectLatest { trade ->

                val instrumentCapitalized = trade.instrument.strValue
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

                fun formatDuration(duration: Duration): String {

                    val durationSeconds = duration.inWholeSeconds

                    return "%02d:%02d:%02d".format(
                        durationSeconds / 3600,
                        (durationSeconds % 3600) / 60,
                        durationSeconds % 60,
                    )
                }

                val duration = when {
                    trade.isClosed -> Details.Duration.Closed(
                        str = formatDuration(trade.exitTimestamp!! - trade.entryTimestamp),
                    )

                    else -> Details.Duration.Open(
                        flow = flow {
                            while (true) {
                                emit(formatDuration(Clock.System.now() - trade.entryTimestamp))
                                delay(1.seconds)
                            }
                        },
                    )
                }

                newExecutionEnabled = !trade.isClosed

                tradingRecord.await().stops.getPrimary(trade.id).collect { stop ->

                    val rValue = stop?.let { trade.rValueAt(pnl = trade.pnl, stop = it) }
                    val rValueStr = rValue?.let { " | ${it.toPlainString()}R" }.orEmpty()

                    val isPartiallyClosed = trade.isClosed || trade.closedQuantity > BigDecimal.ZERO

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
                        exit = trade.averageExit?.toPlainString(),
                        duration = duration,
                        pnl = if (isPartiallyClosed) "${trade.pnl.toPlainString()}$rValueStr" else null,
                        isProfitable = trade.pnl > BigDecimal.ZERO,
                        netPnl = if (isPartiallyClosed) trade.netPnl.toPlainString() else null,
                        isNetProfitable = trade.netPnl > BigDecimal.ZERO,
                        fees = if (isPartiallyClosed) trade.fees.toPlainString() else null,
                    )
                }
            }
        }
    }

    @Composable
    private fun getTradeExecutions(): State<List<Execution>> {
        return produceState(emptyList()) {

            tradingRecord.await().executions.getForTrade(tradeId).collect { executions ->

                value = executions.map { execution ->

                    Execution(
                        id = execution.id,
                        quantity = execution.lots
                            ?.let { "${execution.quantity} ($it ${if (it == 1) "lot" else "lots"})" }
                            ?: execution.quantity.toString(),
                        side = execution.side.strValue.uppercase(),
                        price = execution.price.toPlainString(),
                        timestamp = execution
                            .timestamp
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                            .format(TradeDateTimeFormat),
                        locked = execution.locked,
                    )
                }
            }
        }
    }

    @Composable
    private fun getTradeStops(): State<List<TradeStop>> {
        return produceState(emptyList()) {

            trade.combine(tradingRecord.await().stops.getForTrade(tradeId)) { trade, stops ->

                stops.map { stop ->

                    val brokerage = trade.brokerageAt(stop)

                    TradeStop(
                        price = stop.price,
                        priceText = stop.price.toPlainString(),
                        risk = brokerage.pnl.toPlainString(),
                        netRisk = brokerage.netPNL.toPlainString(),
                        isPrimary = stop.isPrimary,
                    )
                }
            }.collect { tradeStops -> value = tradeStops }
        }
    }

    @Composable
    private fun getTradeTargets(): State<List<TradeTarget>> {
        return produceState(emptyList()) {

            val tradingRecord = tradingProfiles.getRecord(profileId)

            combine(
                trade,
                tradingRecord.targets.getForTrade(tradeId),
                tradingRecord.stops.getPrimary(tradeId),
            ) { trade, targets, stop ->

                targets.map { target ->

                    val brokerage = trade.brokerageAt(target)

                    val pnl = when (trade.side) {
                        TradeSide.Long -> target.price - trade.averageEntry
                        TradeSide.Short -> trade.averageEntry - target.price
                    }.multiply(trade.quantity, MathContext.DECIMAL32)

                    fun BigDecimal.strippedPlainText() = stripTrailingZeros().toPlainString()

                    val rValue = stop?.let { "${trade.rValueAt(pnl, it).toPlainString()}R" }.orEmpty()

                    TradeTarget(
                        price = target.price,
                        priceText = target.price.strippedPlainText(),
                        rValue = rValue,
                        profit = brokerage.pnl.strippedPlainText(),
                        netProfit = brokerage.netPNL.strippedPlainText(),
                        isPrimary = target.isPrimary,
                    )
                }
            }.collect { tradeTargets -> value = tradeTargets }
        }
    }

    @Composable
    private fun showTargetRValues(): Boolean {
        return produceState(false) {
            tradingRecord.await().stops.getPrimary(tradeId).collect { value = it != null }
        }.value
    }

    @Composable
    private fun getExcursions(): State<Excursions?> {
        return produceState<Excursions?>(null) {

            val tradingRecord = tradingProfiles.getRecord(profileId)

            combine(
                tradingRecord.excursions.get(tradeId),
                trade,
                tradingRecord.stops.getPrimary(tradeId),
                tradingRecord.targets.getPrimary(tradeId),
            ) { savedExcursions, trade, stop, target ->

                val excursions = savedExcursions
                    ?: excursionsGenerator.getExcursions(trade, stop, target)
                    ?: return@combine null

                Excursions(
                    maeInTrade = trade.buildExcursionString(stop, excursions, inTrade = true, isMae = true),
                    maeInSession = trade.buildExcursionString(stop, excursions, inTrade = false, isMae = true),
                    mfeInTrade = trade.buildExcursionString(stop, excursions, inTrade = true, isMae = false),
                    mfeInSession = trade.buildExcursionString(stop, excursions, inTrade = false, isMae = false),
                )
            }.collect { excursions -> value = excursions }
        }
    }

    @Composable
    private fun getTradeTags(): State<List<TradeTag>> {
        return produceState(emptyList()) {

            tradingRecord.await()
                .tags
                .getForTrade(tradeId)
                .mapList { tag ->

                    TradeTag(
                        id = tag.id,
                        name = tag.name,
                        description = tag.description.ifBlank { null },
                        color = tag.color?.let(::Color),
                    )
                }
                .collect { value = it }
        }
    }

    @Composable
    private fun getTradeAttachments(): State<List<TradeAttachment>> {
        return produceState(emptyList()) {

            val attachments = tradingRecord.await().attachments

            attachments
                .getForTradeWithFile(tradeId)
                .mapList { attachment ->

                    TradeAttachment(
                        fileId = attachment.fileId,
                        name = attachment.name,
                        description = attachment.description.ifBlank { null },
                        path = attachment.path.toString(),
                        extension = attachment.path.extension.uppercase().ifBlank { null },
                    )
                }
                .collect { value = it }
        }
    }

    @Composable
    private fun getTradeNotes(): State<List<TradeNote>> {
        return produceState(emptyList()) {

            tradingRecord.await().notes.getForTrade(tradeId)
                .mapList { note ->

                    val tz = TimeZone.currentSystemDefault()
                    val added = note.added.toLocalDateTime(tz).format(TradeDateTimeFormat)
                    val lastEdited = note.lastEdited?.toLocalDateTime(tz)?.format(TradeDateTimeFormat)

                    TradeNote(
                        id = note.id,
                        noteText = note.note,
                        dateText = when {
                            lastEdited == null -> "Added $added"
                            else -> "Added $added (Last Edited $lastEdited)"
                        },
                        isMarkdown = note.isMarkdown,
                    )
                }
                .collect { value = it }
        }
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

    private fun onNewExecution(fromExecutionId: TradeExecutionId) {

        tradeContentLauncher.openExecutionForm(
            profileId = profileId,
            formType = TradeExecutionFormType.NewFromExistingInTrade(fromExecutionId),
        )
    }

    private fun onEditExecution(executionId: TradeExecutionId) {

        tradeContentLauncher.openExecutionForm(
            profileId = profileId,
            formType = TradeExecutionFormType.Edit(executionId),
        )
    }

    private fun onLockExecution(executionId: TradeExecutionId) = coroutineScope.launchUnit {

        tradingRecord.await().executions.lock(listOf(executionId))
    }

    private fun onDeleteExecution(executionId: TradeExecutionId) = coroutineScope.launchUnit {

        tradingRecord.await().executions.delete(listOf(executionId))
    }

    private fun onOpenChart() {

        tradeContentLauncher.openTradeReview(profileTradeId)
    }

    private fun onAddStop(price: BigDecimal) = coroutineScope.launchUnit {

        tradingRecord.await().stops.add(tradeId, price)
    }

    private fun onDeleteStop(price: BigDecimal) = coroutineScope.launchUnit {

        tradingRecord.await().stops.delete(tradeId, price)
    }

    private fun setPrimaryStop(price: BigDecimal) = coroutineScope.launchUnit {

        tradingRecord.await().stops.setPrimary(tradeId, price)
    }

    private fun onAddTarget(price: BigDecimal) = coroutineScope.launchUnit {

        tradingRecord.await().targets.add(tradeId, price)
    }

    private fun onDeleteTarget(price: BigDecimal) = coroutineScope.launchUnit {

        tradingRecord.await().targets.delete(tradeId, price)
    }

    private fun setPrimaryTarget(price: BigDecimal) = coroutineScope.launchUnit {

        tradingRecord.await().targets.setPrimary(tradeId, price)
    }

    private fun onAddTag(id: TradeTagId) = coroutineScope.launchUnit {

        tradingRecord.await().tags.add(listOf(tradeId), id)
    }

    private fun onRemoveTag(id: TradeTagId) = coroutineScope.launchUnit {

        tradingRecord.await().tags.remove(tradeId, id)
    }

    private fun onRemoveAttachment(fileId: AttachmentFileId) = coroutineScope.launchUnit {

        tradingRecord.await().attachments.remove(tradeId, fileId)
    }

    private fun onAddNote(
        note: String,
        isMarkdown: Boolean,
    ) = coroutineScope.launchUnit {

        tradingRecord.await().notes.add(tradeId, note, isMarkdown)
    }

    private fun onUpdateNote(
        id: TradeNoteId,
        note: String,
        isMarkdown: Boolean,
    ) = coroutineScope.launchUnit {

        tradingRecord.await().notes.update(id, note, isMarkdown)
    }

    private fun onDeleteNote(id: TradeNoteId) = coroutineScope.launchUnit {

        tradingRecord.await().notes.delete(id)
    }

    private fun Trade.buildExcursionString(
        stop: com.saurabhsandav.core.trades.TradeStop?,
        excursions: TradeExcursions,
        inTrade: Boolean,
        isMae: Boolean,
    ): String {

        fun Trade.getRString(pnl: BigDecimal): String {

            stop ?: return ""

            val rValueStr = rValueAt(stop = stop, pnl = pnl).toPlainString()

            return " | ${rValueStr}R"
        }

        val price = when {
            isMae -> when {
                inTrade -> excursions.tradeMaePrice
                else -> excursions.sessionMaePrice
            }

            else -> when {
                inTrade -> excursions.tradeMfePrice
                else -> excursions.sessionMfePrice
            }
        }

        val pnl = when {
            isMae -> when {
                inTrade -> excursions.tradeMaePnl
                else -> excursions.sessionMaePnl
            }

            else -> when {
                inTrade -> excursions.tradeMfePnl
                else -> excursions.sessionMfePnl
            }
        }

        val rStr = when {
            isMae -> when {
                inTrade -> getRString(excursions.tradeMaePnl)
                else -> getRString(excursions.sessionMaePnl)
            }

            else -> when {
                inTrade -> getRString(excursions.tradeMfePnl)
                else -> getRString(excursions.sessionMfePnl)
            }
        }

        return "${price.toPlainString()} | ${pnl.toPlainString()}$rStr"
    }
}
