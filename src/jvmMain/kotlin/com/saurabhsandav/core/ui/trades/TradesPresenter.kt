package com.saurabhsandav.core.ui.trades

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.trades.Trade
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.model.TradeId
import com.saurabhsandav.core.ui.common.TradeDateTimeFormatter
import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import com.saurabhsandav.core.ui.tradecontent.TradeContentLauncher
import com.saurabhsandav.core.ui.trades.model.TradesEvent
import com.saurabhsandav.core.ui.trades.model.TradesEvent.OpenChart
import com.saurabhsandav.core.ui.trades.model.TradesEvent.OpenDetails
import com.saurabhsandav.core.ui.trades.model.TradesState
import com.saurabhsandav.core.ui.trades.model.TradesState.TradeEntry
import com.saurabhsandav.core.utils.emitInto
import com.saurabhsandav.core.utils.format
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Stable
internal class TradesPresenter(
    coroutineScope: CoroutineScope,
    private val profileId: ProfileId,
    private val tradeContentLauncher: TradeContentLauncher,
    private val tradingProfiles: TradingProfiles,
) {

    private val errors = mutableStateListOf<UIErrorMessage>()

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule TradesState(
            openTrades = getOpenTrades().value,
            todayTrades = getTodayTrades().value,
            pastTrades = getPastTrades().value,
            errors = remember(errors) { errors.toImmutableList() },
            eventSink = ::onEvent,
        )
    }

    private fun onEvent(event: TradesEvent) {

        when (event) {
            is OpenDetails -> onOpenDetails(event.id)
            is OpenChart -> onOpenChart(event.id)
        }
    }

    @Composable
    private fun getOpenTrades(): State<ImmutableList<TradeEntry>> {
        return remember {
            flow {

                tradingProfiles
                    .getRecord(profileId)
                    .trades
                    .getOpen()
                    .map { trades ->
                        trades
                            .map { it.toTradeListEntry() }
                            .toImmutableList()
                    }
                    .emitInto(this)
            }
        }.collectAsState(persistentListOf())
    }

    @Composable
    private fun getTodayTrades(): State<ImmutableList<TradeEntry>> {
        return remember {
            flow {

                tradingProfiles
                    .getRecord(profileId)
                    .trades
                    .getToday()
                    .map { trades ->
                        trades
                            .map { it.toTradeListEntry() }
                            .toImmutableList()
                    }
                    .emitInto(this)
            }
        }.collectAsState(persistentListOf())
    }

    @Composable
    private fun getPastTrades(): State<ImmutableList<TradeEntry>> {
        return remember {
            flow {

                tradingProfiles
                    .getRecord(profileId)
                    .trades
                    .getBeforeToday()
                    .map { trades ->
                        trades
                            .map { it.toTradeListEntry() }
                            .toImmutableList()
                    }
                    .emitInto(this)
            }
        }.collectAsState(persistentListOf())
    }

    private fun Trade.toTradeListEntry(): TradeEntry {

        val instrumentCapitalized = instrument.strValue
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
            isClosed -> flowOf(formatDuration(exitTimestamp!! - entryTimestamp))
            else -> flow {
                while (true) {
                    emit(formatDuration(Clock.System.now() - entryTimestamp))
                    delay(1.seconds)
                }
            }
        }

        return TradeEntry(
            id = id,
            broker = "$broker ($instrumentCapitalized)",
            ticker = ticker,
            side = side.toString().uppercase(),
            quantity = when {
                !isClosed -> "$closedQuantity / $quantity"
                else -> quantity.toPlainString()
            },
            entry = averageEntry.toPlainString(),
            exit = averageExit?.toPlainString() ?: "",
            entryTime = TradeDateTimeFormatter.format(
                ldt = entryTimestamp.toLocalDateTime(TimeZone.currentSystemDefault())
            ),
            duration = durationStr,
            pnl = pnl.toPlainString(),
            isProfitable = pnl > BigDecimal.ZERO,
            netPnl = netPnl.toPlainString(),
            isNetProfitable = netPnl > BigDecimal.ZERO,
            fees = fees.toPlainString(),
        )
    }

    private fun onOpenDetails(id: TradeId) {

        tradeContentLauncher.openTrade(ProfileTradeId(profileId = profileId, tradeId = id))
    }

    private fun onOpenChart(id: TradeId) {

        tradeContentLauncher.openTradeReview(ProfileTradeId(profileId = profileId, tradeId = id))
    }
}
