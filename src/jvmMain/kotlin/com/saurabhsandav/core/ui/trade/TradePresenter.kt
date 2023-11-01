package com.saurabhsandav.core.ui.trade

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.brokerageAt
import com.saurabhsandav.core.trades.model.*
import com.saurabhsandav.core.ui.TradeContentLauncher
import com.saurabhsandav.core.ui.common.TradeDateTimeFormatter
import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.trade.model.AttachmentFormModel
import com.saurabhsandav.core.ui.trade.model.TradeEvent
import com.saurabhsandav.core.ui.trade.model.TradeEvent.*
import com.saurabhsandav.core.ui.trade.model.TradeState
import com.saurabhsandav.core.ui.trade.model.TradeState.*
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType
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
    private val profileId: ProfileId,
    private val tradeId: TradeId,
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
            attachments = getTradeAttachments().value,
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
            is AddAttachment -> onAddAttachment(event.formModel)
            is UpdateAttachment -> onUpdateAttachment(event.id, event.formModel)
            is RemoveAttachment -> onRemoveAttachment(event.id)
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
                        note = note.note,
                        dateText = when {
                            lastEdited == null -> "Added $added"
                            else -> "Added $added (Last Edited $lastEdited)"
                        },
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

    private fun onAddNote(note: String) = coroutineScope.launchUnit {

        tradingProfiles.getRecord(profileId).trades.addNote(tradeId, note)
    }

    private fun onUpdateNote(id: TradeNoteId, note: String) = coroutineScope.launchUnit {

        tradingProfiles.getRecord(profileId).trades.updateNote(id, note)
    }

    private fun onDeleteNote(id: TradeNoteId) = coroutineScope.launchUnit {

        tradingProfiles.getRecord(profileId).trades.deleteNote(id)
    }
}
