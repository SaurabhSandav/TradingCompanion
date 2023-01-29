package ui.trades

import AppModule
import androidx.compose.runtime.*
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toJavaLocalDate
import trades.TradesRepo
import trades.model.Trade
import ui.common.CollectEffect
import ui.common.UIErrorMessage
import ui.trades.model.TradeListItem
import ui.trades.model.TradesEvent
import ui.trades.model.TradesState
import java.math.BigDecimal
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

internal class TradesPresenter(
    private val coroutineScope: CoroutineScope,
    private val appModule: AppModule,
    private val tradesRepo: TradesRepo = TradesRepo(appModule),
) {

    private val events = MutableSharedFlow<TradesEvent>(extraBufferCapacity = Int.MAX_VALUE)

    val state = coroutineScope.launchMolecule(RecompositionClock.ContextClock) {

        CollectEffect(events) { event ->

            when (event) {
                else -> Unit
            }
        }

        return@launchMolecule TradesState(
            tradesItems = getTradeListEntries().value,
        )
    }

    val errors = mutableStateListOf<UIErrorMessage>()

    fun event(event: TradesEvent) {
        events.tryEmit(event)
    }

    @Composable
    private fun getTradeListEntries(): State<Map<TradeListItem.DayHeader, List<TradeListItem.Entry>>> {
        return remember {
            tradesRepo.allTrades.map { trades ->
                trades
                    .groupBy { it.entryTimestamp.date }
                    .mapKeys { (date, _) -> date.toTradeListDayHeader() }
                    .mapValues { (_, list) -> list.map { it.toTradeListEntry() } }
            }
        }.collectAsState(emptyMap())
    }

    private fun LocalDate.toTradeListDayHeader(): TradeListItem.DayHeader {
        val formatted = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(toJavaLocalDate())
        return TradeListItem.DayHeader(formatted)
    }

    private fun Trade.toTradeListEntry(): TradeListItem.Entry {

        val instrumentCapitalized = instrument
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

        val timeZone = TimeZone.of("Asia/Kolkata")
        val entryInstant = entryTimestamp.toInstant(timeZone)
        val exitInstant = exitTimestamp?.toInstant(timeZone)
        val s = exitInstant?.let { (it - entryInstant).inWholeSeconds }

        val duration = s?.let { "%02d:%02d:%02d".format(it / 3600, (it % 3600) / 60, (it % 60)) }

        return TradeListItem.Entry(
            id = id,
            broker = "$broker ($instrumentCapitalized)",
            ticker = ticker,
            side = side.toString().uppercase(),
            quantity = (lots?.let { "$closedQuantity / $quantity ($it ${if (it == 1) "lot" else "lots"})" }
                ?: "$closedQuantity / $quantity").toString(),
            entry = averageEntry.toPlainString(),
            exit = averageExit?.toPlainString() ?: "",
            duration = "${entryTimestamp.time} -> ${exitTimestamp?.time ?: "Now"}\n${duration?.let { "($it)" }}",
            pnl = pnl.toPlainString(),
            isProfitable = pnl > BigDecimal.ZERO,
            netPnl = netPnl.toPlainString(),
            isNetProfitable = netPnl > BigDecimal.ZERO,
            fees = fees.toPlainString(),
        )
    }
}
