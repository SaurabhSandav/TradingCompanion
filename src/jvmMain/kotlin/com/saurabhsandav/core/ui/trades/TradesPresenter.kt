package com.saurabhsandav.core.ui.trades

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.trades.Trade
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.common.TradeDateTimeFormatter
import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import com.saurabhsandav.core.ui.tradecontent.TradeContentLauncher
import com.saurabhsandav.core.ui.trades.model.TradesEvent
import com.saurabhsandav.core.ui.trades.model.TradesEvent.OpenChart
import com.saurabhsandav.core.ui.trades.model.TradesEvent.OpenDetails
import com.saurabhsandav.core.ui.trades.model.TradesState
import com.saurabhsandav.core.ui.trades.model.TradesState.TradeEntry
import com.saurabhsandav.core.utils.format
import com.saurabhsandav.core.utils.getCurrentTradingProfile
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flatMapLatest
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
    private val tradeContentLauncher: TradeContentLauncher,
    private val appPrefs: FlowSettings,
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
            is OpenDetails -> onOpenDetails(event.profileTradeId)
            is OpenChart -> onOpenChart(event.profileTradeId)
        }
    }

    @Composable
    private fun getOpenTrades(): State<ImmutableList<TradeEntry>> {
        return remember {
            appPrefs.getCurrentTradingProfile(tradingProfiles).flatMapLatest { profile ->

                val tradingRecord = tradingProfiles.getRecord(profile.id)

                tradingRecord.trades.getOpen().map { trades ->
                    trades
                        .map { it.toTradeListEntry(profile.id) }
                        .toImmutableList()
                }
            }
        }.collectAsState(persistentListOf())
    }

    @Composable
    private fun getTodayTrades(): State<ImmutableList<TradeEntry>> {
        return remember {
            appPrefs.getCurrentTradingProfile(tradingProfiles).flatMapLatest { profile ->

                val tradingRecord = tradingProfiles.getRecord(profile.id)

                tradingRecord.trades.getToday().map { trades ->
                    trades
                        .map { it.toTradeListEntry(profile.id) }
                        .toImmutableList()
                }
            }
        }.collectAsState(persistentListOf())
    }

    @Composable
    private fun getPastTrades(): State<ImmutableList<TradeEntry>> {
        return remember {
            appPrefs.getCurrentTradingProfile(tradingProfiles).flatMapLatest { profile ->

                val tradingRecord = tradingProfiles.getRecord(profile.id)

                tradingRecord.trades.getBeforeToday().map { trades ->
                    trades
                        .map { it.toTradeListEntry(profile.id) }
                        .toImmutableList()
                }
            }
        }.collectAsState(persistentListOf())
    }

    private fun Trade.toTradeListEntry(profileId: ProfileId): TradeEntry {

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
            profileTradeId = ProfileTradeId(profileId = profileId, tradeId = id),
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

    private fun onOpenDetails(profileTradeId: ProfileTradeId) {

        tradeContentLauncher.openTrade(profileTradeId)
    }

    private fun onOpenChart(profileTradeId: ProfileTradeId) {

        tradeContentLauncher.openTradeReview(profileTradeId)
    }
}
