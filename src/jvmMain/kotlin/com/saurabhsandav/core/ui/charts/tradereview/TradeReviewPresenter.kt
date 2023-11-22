package com.saurabhsandav.core.ui.charts.tradereview

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.trades.Trade
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.charts.tradereview.model.TradeReviewEvent
import com.saurabhsandav.core.ui.charts.tradereview.model.TradeReviewEvent.*
import com.saurabhsandav.core.ui.charts.tradereview.model.TradeReviewState
import com.saurabhsandav.core.ui.charts.tradereview.model.TradeReviewState.MarkedTradeEntry
import com.saurabhsandav.core.ui.charts.tradereview.model.TradeReviewState.TradeEntry
import com.saurabhsandav.core.ui.common.TradeDateTimeFormatter
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import com.saurabhsandav.core.ui.tradecontent.TradeContentLauncher
import com.saurabhsandav.core.utils.PrefKeys
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
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Stable
internal class TradeReviewPresenter(
    private val coroutineScope: CoroutineScope,
    initialMarkedTrades: List<ProfileTradeId>,
    private val onOpenChart: (
        ticker: String,
        start: Instant,
        end: Instant?,
    ) -> Unit,
    private val onMarkTrades: (tradeIds: List<ProfileTradeId>) -> Unit,
    private val tradeContentLauncher: TradeContentLauncher,
    private val tradingProfiles: TradingProfiles,
    private val appPrefs: FlowSettings,
) {

    private val selectedProfileId = appPrefs.getLongOrNullFlow(PrefKeys.TradeReviewTradingProfile)
        .map { it?.let(::ProfileId) }
        .stateIn(coroutineScope, SharingStarted.Eagerly, null)

    private val markedTradeIds = initialMarkedTrades.toMutableStateList()

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule TradeReviewState(
            selectedProfileId = selectedProfileId.collectAsState().value,
            trades = getTrades().value,
            markedTrades = getMarkedTrades().value,
            eventSink = ::onEvent,
        )
    }

    private fun onEvent(event: TradeReviewEvent) {

        when (event) {
            is SelectProfile -> onSelectProfile(event.id)
            is MarkTrade -> onMarkTrade(event.profileTradeId, event.isMarked)
            is SelectTrade -> onSelectTrade(event.profileTradeId)
            is OpenDetails -> onOpenDetails(event.profileTradeId)
            ClearMarkedTrades -> onClearMarkedTrades()
        }
    }

    @Composable
    private fun getTrades(): State<ImmutableList<TradeEntry>> {
        return remember {

            selectedProfileId
                .flatMapLatest { id ->
                    if (id != null) tradingProfiles.getProfileOrNull(id) else flowOf(null)
                }
                .filterNotNull()
                .flatMapLatest { profile ->

                    val tradingRecord = tradingProfiles.getRecord(profile.id)

                    tradingRecord.trades
                        .allTrades
                        .combine(snapshotFlow { markedTradeIds.toList() }) { trades, markedProfileTradeIds ->
                            trades
                                .map {

                                    val profileTradeId = ProfileTradeId(
                                        profileId = profile.id,
                                        tradeId = it.id,
                                    )

                                    it.toTradeListEntry(
                                        profileTradeId = profileTradeId,
                                        isMarked = profileTradeId in markedProfileTradeIds,
                                    )
                                }
                                .toImmutableList()
                        }
                }
        }.collectAsState(persistentListOf())
    }

    private fun Trade.toTradeListEntry(
        profileTradeId: ProfileTradeId,
        isMarked: Boolean,
    ): TradeEntry {

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
            profileTradeId = profileTradeId,
            isMarked = isMarked,
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
        )
    }

    @Composable
    private fun getMarkedTrades(): State<ImmutableList<MarkedTradeEntry>> {
        return remember {

            snapshotFlow { markedTradeIds.toList() }
                .flatMapLatest { profileTradeIds ->

                    profileTradeIds
                        .groupBy(
                            keySelector = { it.profileId },
                            valueTransform = { it.tradeId },
                        )
                        .map { (profileId, tradeIds) ->

                            val tradingRecord = tradingProfiles.getRecord(profileId)

                            tradingProfiles.getProfile(profileId).flatMapLatest { profile ->

                                tradingRecord.trades
                                    .getByIds(ids = tradeIds)
                                    .mapList {
                                        it.toMarkedTradeListEntry(
                                            profileId = profileId,
                                            profileName = profile.name,
                                        )
                                    }
                            }
                        }
                        .let { flows ->
                            when {
                                flows.isEmpty() -> flowOf(emptyList())
                                else -> combine(flows) { it.toList().flatten() }
                            }
                        }
                        .map { it.toImmutableList() }
                }
        }.collectAsState(persistentListOf())
    }

    private fun Trade.toMarkedTradeListEntry(
        profileId: ProfileId,
        profileName: String,
    ): MarkedTradeEntry {

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

        return MarkedTradeEntry(
            profileTradeId = ProfileTradeId(profileId = profileId, tradeId = id),
            profileName = profileName,
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
        )
    }

    private fun onSelectProfile(id: ProfileId) = coroutineScope.launchUnit {

        // Save selected profile
        appPrefs.putLong(PrefKeys.TradeReviewTradingProfile, id.value)
    }

    private fun onMarkTrade(
        profileTradeId: ProfileTradeId,
        isMarked: Boolean,
    ) {

        when {
            isMarked -> if (profileTradeId !in markedTradeIds) markedTradeIds.add(profileTradeId)
            else -> markedTradeIds.remove(profileTradeId)
        }

        onMarkTrades(markedTradeIds)
    }

    private fun onSelectTrade(profileTradeId: ProfileTradeId) = coroutineScope.launchUnit {

        // Mark selected trade
        if (profileTradeId !in markedTradeIds) {
            markedTradeIds.add(profileTradeId)
            onMarkTrades(markedTradeIds)
        }

        val tradingRecord = tradingProfiles.getRecord(profileTradeId.profileId)

        val trade = tradingRecord.trades.getById(profileTradeId.tradeId).first()
        val start = trade.entryTimestamp
        val end = trade.exitTimestamp

        // Show trade on chart
        onOpenChart(trade.ticker, start, end)
    }

    private fun onOpenDetails(profileTradeId: ProfileTradeId) {

        tradeContentLauncher.openTrade(profileTradeId)
    }

    private fun onClearMarkedTrades() {
        markedTradeIds.clear()
        onMarkTrades(markedTradeIds)
    }
}
