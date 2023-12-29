package com.saurabhsandav.core.ui.trade

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.trades.*
import com.saurabhsandav.core.trades.model.*
import com.saurabhsandav.core.ui.common.TradeDateTimeFormatter
import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.trade.model.AttachmentFormModel
import com.saurabhsandav.core.ui.trade.model.TradeEvent
import com.saurabhsandav.core.ui.trade.model.TradeEvent.*
import com.saurabhsandav.core.ui.trade.model.TradeState
import com.saurabhsandav.core.ui.trade.model.TradeState.*
import com.saurabhsandav.core.ui.trade.model.TradeState.TradeAttachment
import com.saurabhsandav.core.ui.trade.model.TradeState.TradeNote
import com.saurabhsandav.core.ui.trade.model.TradeState.TradeStop
import com.saurabhsandav.core.ui.trade.model.TradeState.TradeTag
import com.saurabhsandav.core.ui.trade.model.TradeState.TradeTarget
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import com.saurabhsandav.core.ui.tradecontent.TradeContentLauncher
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType
import com.saurabhsandav.core.utils.emitInto
import com.saurabhsandav.core.utils.format
import com.saurabhsandav.core.utils.launchUnit
import com.saurabhsandav.core.utils.mapList
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import java.util.*
import kotlin.io.path.extension
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Stable
internal class TradePresenter(
    private val profileTradeId: ProfileTradeId,
    private val coroutineScope: CoroutineScope,
    private val tradeContentLauncher: TradeContentLauncher,
    private val tradingProfiles: TradingProfiles,
) {

    private val profileId = profileTradeId.profileId
    private val tradeId = profileTradeId.tradeId

    private val trade = flow { tradingProfiles.getRecord(profileId).trades.getById(tradeId).emitInto(this) }.shareIn(
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
            excursions = getExcursions().value,
            tags = getTradeTags().value,
            tagSuggestions = ::tagSuggestions,
            attachments = getTradeAttachments().value,
            notes = getTradeNotes().value,
            eventSink = ::onEvent,
        )
    }

    val errors = mutableStateListOf<UIErrorMessage>()

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
            is AddTarget -> onAddTarget(event.price)
            is DeleteTarget -> onDeleteTarget(event.price)
            is AddTag -> onAddTag(event.id)
            is RemoveTag -> onRemoveTag(event.id)
            is AddAttachment -> onAddAttachment(event.formModel)
            is UpdateAttachment -> onUpdateAttachment(event.id, event.formModel)
            is RemoveAttachment -> onRemoveAttachment(event.id)
            is AddNote -> onAddNote(event.note, event.isMarkdown)
            is UpdateNote -> onUpdateNote(event.id, event.note, event.isMarkdown)
            is DeleteNote -> onDeleteNote(event.id)
        }
    }

    @Composable
    private fun getTradeDetail(): State<Details?> {
        return produceState<Details?>(null) {

            val record = tradingProfiles.getRecord(profileId)

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

                val durationStr = when {
                    trade.isClosed -> flowOf(formatDuration(trade.exitTimestamp!! - trade.entryTimestamp))
                    else -> flow {
                        while (true) {
                            emit(formatDuration(Clock.System.now() - trade.entryTimestamp))
                            delay(1.seconds)
                        }
                    }
                }

                newExecutionEnabled = !trade.isClosed

                record.trades.getPrimaryStop(trade.id).collect { stop ->

                    val rValue = stop?.let { trade.rValueAt(pnl = trade.pnl, stop = it) }
                    val rValueStr = rValue?.let { " | ${it.toPlainString()}R" }.orEmpty()

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
                        pnl = "${trade.pnl.toPlainString()}${rValueStr}",
                        isProfitable = trade.pnl > BigDecimal.ZERO,
                        netPnl = trade.netPnl.toPlainString(),
                        isNetProfitable = trade.netPnl > BigDecimal.ZERO,
                        fees = trade.fees.toPlainString(),
                    )
                }
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
                        timestamp = TradeDateTimeFormatter.format(
                            ldt = execution.timestamp.toLocalDateTime(TimeZone.currentSystemDefault())
                        ),
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

                    val brokerage = trade.brokerageAt(stop)

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

                    val brokerage = trade.brokerageAt(target)

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
    private fun getExcursions(): State<Excursions?> {
        return produceState<Excursions?>(null) {

            val tradesRepo = tradingProfiles.getRecord(profileId).trades

            tradesRepo.getExcursions(tradeId).collectLatest { excursions ->

                if (excursions == null) {
                    value = null
                    return@collectLatest
                }

                trade.collectLatest { trade ->

                    tradesRepo.getPrimaryStop(tradeId).collect { stop ->

                        fun buildExcursionString(inTrade: Boolean, isMae: Boolean): String =
                            trade.buildExcursionString(stop, excursions, inTrade = inTrade, isMae = isMae)

                        value = Excursions(
                            maeInTrade = buildExcursionString(inTrade = true, isMae = true),
                            maeInSession = buildExcursionString(inTrade = false, isMae = true),
                            mfeInTrade = buildExcursionString(inTrade = true, isMae = false),
                            mfeInSession = buildExcursionString(inTrade = false, isMae = false),
                        )
                    }
                }
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
    private fun getTradeAttachments(): State<ImmutableList<TradeAttachment>> {
        return produceState<ImmutableList<TradeAttachment>>(persistentListOf()) {

            val tradingRecord = tradingProfiles.getRecord(profileId)

            tradingRecord.trades
                .getAttachmentsForTrade(tradeId)
                .mapList { attachment ->

                    val path = tradingRecord.trades.attachmentsPath.resolve(attachment.fileName)

                    TradeAttachment(
                        id = attachment.id,
                        name = attachment.name,
                        description = attachment.description.ifBlank { null },
                        path = path.toString(),
                        extension = path.extension.uppercase().ifBlank { null },
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

                    val added = TradeDateTimeFormatter.format(
                        ldt = note.added.toLocalDateTime(TimeZone.currentSystemDefault())
                    )
                    val lastEdited = note.lastEdited
                        ?.toLocalDateTime(TimeZone.currentSystemDefault())
                        ?.let(TradeDateTimeFormatter::format)

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

                    val brokerage = trade.brokerageAt(price)

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

                    val brokerage = trade.brokerageAt(price)

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
            .emitInto(this)
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

        val tradingRecord = tradingProfiles.getRecord(profileId)

        tradingRecord.executions.lock(listOf(executionId))
    }

    private fun onDeleteExecution(executionId: TradeExecutionId) = coroutineScope.launchUnit {

        val tradingRecord = tradingProfiles.getRecord(profileId)

        tradingRecord.executions.delete(listOf(executionId))
    }

    private fun onOpenChart() {

        tradeContentLauncher.openTradeReview(profileTradeId)
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

    private fun onAddTag(id: TradeTagId) = coroutineScope.launchUnit {

        tradingProfiles.getRecord(profileId).trades.addTag(tradeId, id)
    }

    private fun onRemoveTag(id: TradeTagId) = coroutineScope.launchUnit {

        tradingProfiles.getRecord(profileId).trades.removeTag(tradeId, id)
    }

    private fun onAddAttachment(formModel: AttachmentFormModel) = coroutineScope.launchUnit {

        tradingProfiles.getRecord(profileId).trades.addAttachment(
            tradeId = tradeId,
            name = formModel.nameField.value,
            description = formModel.descriptionField.value,
            pathStr = formModel.path,
        )
    }

    private fun onUpdateAttachment(
        id: TradeAttachmentId,
        formModel: AttachmentFormModel,
    ) = coroutineScope.launchUnit {

        tradingProfiles.getRecord(profileId).trades.updateAttachment(
            tradeId = tradeId,
            attachmentId = id,
            name = formModel.nameField.value,
            description = formModel.descriptionField.value,
        )
    }

    private fun onRemoveAttachment(id: TradeAttachmentId) = coroutineScope.launchUnit {

        tradingProfiles.getRecord(profileId).trades.removeAttachment(tradeId, id)
    }

    private fun onAddNote(note: String, isMarkdown: Boolean) = coroutineScope.launchUnit {

        tradingProfiles.getRecord(profileId).trades.addNote(tradeId, note, isMarkdown)
    }

    private fun onUpdateNote(id: TradeNoteId, note: String, isMarkdown: Boolean) = coroutineScope.launchUnit {

        tradingProfiles.getRecord(profileId).trades.updateNote(id, note, isMarkdown)
    }

    private fun onDeleteNote(id: TradeNoteId) = coroutineScope.launchUnit {

        tradingProfiles.getRecord(profileId).trades.deleteNote(id)
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
