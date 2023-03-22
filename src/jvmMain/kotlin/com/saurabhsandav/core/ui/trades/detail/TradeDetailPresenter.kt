package com.saurabhsandav.core.ui.trades.detail

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trades.TradesRepo
import com.saurabhsandav.core.ui.common.CollectEffect
import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.trades.detail.model.TradeDetailEvent
import com.saurabhsandav.core.ui.trades.detail.model.TradeDetailEvent.*
import com.saurabhsandav.core.ui.trades.detail.model.TradeDetailState
import com.saurabhsandav.core.ui.trades.detail.model.TradeDetailState.*
import com.saurabhsandav.core.utils.launchUnit
import com.saurabhsandav.core.utils.mapList
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import java.time.format.DateTimeFormatter
import java.util.*

@Stable
internal class TradeDetailPresenter(
    private val tradeId: Long,
    private val coroutineScope: CoroutineScope,
    private val appModule: AppModule,
    private val tradesRepo: TradesRepo = appModule.tradesRepo,
) {

    private val events = MutableSharedFlow<TradeDetailEvent>(extraBufferCapacity = Int.MAX_VALUE)

    val state = coroutineScope.launchMolecule(RecompositionClock.ContextClock) {

        CollectEffect(events) { event ->

            when (event) {
                is AddStop -> onAddStop(event.price)
                is DeleteStop -> onDeleteStop(event.price)
                is AddTarget -> onAddTarget(event.price)
                is DeleteTarget -> onDeleteTarget(event.price)
                is AddNote -> onAddNote(event.note)
                is UpdateNote -> onUpdateNote(event.id, event.note)
                is DeleteNote -> onDeleteNote(event.id)
            }
        }

        return@launchMolecule TradeDetailState(
            tradeDetail = getTradeDetail().value,
            stops = getTradeStops().value,
            targets = getTradeTargets().value,
            notes = getTradeNotes().value,
        )
    }

    val errors = mutableStateListOf<UIErrorMessage>()

    fun event(event: TradeDetailEvent) {
        events.tryEmit(event)
    }

    @Composable
    private fun getTradeDetail(): State<TradeDetail?> {
        return remember {
            tradesRepo.getById(tradeId).map { trade ->

                val instrumentCapitalized = trade.instrument
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

                val timeZone = TimeZone.of("Asia/Kolkata")
                val entryInstant = trade.entryTimestamp.toInstant(timeZone)
                val exitInstant = trade.exitTimestamp?.toInstant(timeZone)
                val s = exitInstant?.let { (it - entryInstant).inWholeSeconds }

                val duration = s?.let { "%02d:%02d:%02d".format(it / 3600, (it % 3600) / 60, (it % 60)) }

                TradeDetail(
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
        }.collectAsState(null)
    }

    @Composable
    private fun getTradeStops(): State<ImmutableList<TradeStop>> {
        return remember {
            tradesRepo.getStopsForTrade(tradeId).map { stops ->

                stops.map { stop ->
                    TradeStop(
                        price = stop.price,
                        priceText = stop.price.toPlainString(),
                        risk = stop.risk.toPlainString(),
                    )
                }.toImmutableList()
            }
        }.collectAsState(persistentListOf())
    }

    @Composable
    private fun getTradeTargets(): State<ImmutableList<TradeTarget>> {
        return remember {
            tradesRepo.getTargetsForTrade(tradeId).map { targets ->

                targets.map { target ->
                    TradeTarget(
                        price = target.price,
                        priceText = target.price.toPlainString(),
                        profit = target.profit.toPlainString(),
                    )
                }.toImmutableList()
            }
        }.collectAsState(persistentListOf())
    }

    @Composable
    private fun getTradeNotes(): State<ImmutableList<TradeNote>> {
        return remember {
            tradesRepo.getNotesForTrade(tradeId)
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
                .map { it.toImmutableList() }
        }.collectAsState(persistentListOf())
    }

    private fun onAddStop(price: BigDecimal) = coroutineScope.launchUnit {
        tradesRepo.addStop(tradeId, price)
    }

    private fun onDeleteStop(price: BigDecimal) = coroutineScope.launchUnit {
        tradesRepo.deleteStop(tradeId, price)
    }

    private fun onAddTarget(price: BigDecimal) = coroutineScope.launchUnit {
        tradesRepo.addTarget(tradeId, price)
    }

    private fun onDeleteTarget(price: BigDecimal) = coroutineScope.launchUnit {
        tradesRepo.deleteTarget(tradeId, price)
    }

    private fun onAddNote(note: String) = coroutineScope.launchUnit {
        tradesRepo.addNote(tradeId, note)
    }

    private fun onUpdateNote(id: Long, note: String) = coroutineScope.launchUnit {
        tradesRepo.updateNote(id, note)
    }

    private fun onDeleteNote(id: Long) = coroutineScope.launchUnit {
        tradesRepo.deleteNote(id)
    }
}
