package com.saurabhsandav.core.ui.tradereview

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.trades.Trade
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.model.TradeFilter
import com.saurabhsandav.core.ui.charts.ChartsHandle
import com.saurabhsandav.core.ui.common.TradeDateTimeFormatter
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import com.saurabhsandav.core.ui.tradecontent.TradeContentLauncher
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewEvent
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewEvent.*
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewState
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewState.MarkedTradeItem
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewState.TradeItem
import com.saurabhsandav.core.utils.PrefKeys
import com.saurabhsandav.core.utils.format
import com.saurabhsandav.core.utils.launchUnit
import com.saurabhsandav.core.utils.mapList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class TradeReviewPresenter(
    private val coroutineScope: CoroutineScope,
    private val chartsHandle: ChartsHandle,
    private val tradeContentLauncher: TradeContentLauncher,
    private val tradingProfiles: TradingProfiles,
    appPrefs: FlowSettings,
) {

    private val selectedProfileId = MutableStateFlow<ProfileId?>(null)

    private val markedTradeIds = chartsHandle.markedTradeIds.toMutableStateList()
    private val tradeFilter = MutableStateFlow(TradeFilter())

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule TradeReviewState(
            selectedProfileId = selectedProfileId.collectAsState().value,
            selectedProfileName = getSelectedProfileName(),
            trades = getTrades().value,
            markedTrades = getMarkedTrades().value,
            eventSink = ::onEvent,
        )
    }

    init {

        coroutineScope.launch {

            val defaultProfileId = appPrefs.getLongOrNullFlow(PrefKeys.CurrentTradingProfile)
                .first()
                ?.let(::ProfileId)

            selectedProfileId.update { it ?: defaultProfileId }
        }
    }

    private fun onEvent(event: TradeReviewEvent) {

        when (event) {
            is ProfileSelected -> onProfileSelected(event.id)
            is MarkTrade -> onMarkTrade(event.profileTradeId, event.isMarked)
            is SelectTrade -> onSelectTrade(event.profileTradeId)
            is OpenDetails -> onOpenDetails(event.profileTradeId)
            ClearMarkedTrades -> onClearMarkedTrades()
            is ApplyFilter -> onApplyFilter(event.tradeFilter)
        }
    }

    @Composable
    private fun getSelectedProfileName(): String? {
        return remember {

            selectedProfileId.flatMapLatest { profileId ->
                when (profileId) {
                    null -> flowOf(null)
                    else -> tradingProfiles.getProfileOrNull(profileId).map { it?.name }
                }
            }
        }.collectAsState(null).value
    }

    @Composable
    private fun getTrades(): State<List<TradeItem>> {
        return remember {

            selectedProfileId
                .flatMapLatest { id ->
                    if (id != null) tradingProfiles.getProfileOrNull(id) else flowOf(null)
                }
                .flatMapLatest { profile ->

                    if (profile == null) return@flatMapLatest flowOf(emptyList())

                    val tradesRepo = tradingProfiles.getRecord(profile.id).trades

                    tradeFilter
                        .flatMapLatest(tradesRepo::getFiltered)
                        .combine(snapshotFlow { markedTradeIds.toList() }) { trades, markedProfileTradeIds ->
                            trades
                                .map {

                                    val profileTradeId = ProfileTradeId(
                                        profileId = profile.id,
                                        tradeId = it.id,
                                    )

                                    it.toTradeItem(
                                        profileTradeId = profileTradeId,
                                        isMarked = profileTradeId in markedProfileTradeIds,
                                    )
                                }
                        }
                }
        }.collectAsState(emptyList())
    }

    private fun Trade.toTradeItem(
        profileTradeId: ProfileTradeId,
        isMarked: Boolean,
    ): TradeItem {

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

        return TradeItem(
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
    private fun getMarkedTrades(): State<List<MarkedTradeItem>> {
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

                            tradingProfiles.getProfileOrNull(profileId).flatMapLatest profile@{ profile ->

                                if (profile == null) {
                                    markedTradeIds.removeIf { it.profileId == profileId }
                                    chartsHandle.setMarkedTrades(markedTradeIds)
                                    return@profile emptyFlow()
                                }

                                tradingRecord
                                    .trades
                                    .getByIds(ids = tradeIds)
                                    .mapList {
                                        it.toMarkedTradeItem(
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
                }
        }.collectAsState(emptyList())
    }

    private fun Trade.toMarkedTradeItem(
        profileId: ProfileId,
        profileName: String,
    ): MarkedTradeItem {

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

        return MarkedTradeItem(
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

    private fun onProfileSelected(id: ProfileId?) {
        selectedProfileId.value = id
    }

    private fun onMarkTrade(
        profileTradeId: ProfileTradeId,
        isMarked: Boolean,
    ) {

        when {
            isMarked -> if (profileTradeId !in markedTradeIds) markedTradeIds.add(profileTradeId)
            else -> markedTradeIds.remove(profileTradeId)
        }

        chartsHandle.setMarkedTrades(markedTradeIds)
    }

    private fun onSelectTrade(profileTradeId: ProfileTradeId) = coroutineScope.launchUnit {

        // Select trade profile as current profile
        onProfileSelected(profileTradeId.profileId)

        // Mark selected trade
        if (profileTradeId !in markedTradeIds) {
            markedTradeIds.add(profileTradeId)
            chartsHandle.setMarkedTrades(markedTradeIds)
        }

        val tradingRecord = tradingProfiles.getRecord(profileTradeId.profileId)

        val trade = tradingRecord.trades.getById(profileTradeId.tradeId).first()
        val start = trade.entryTimestamp
        val end = trade.exitTimestamp

        // Show trade on chart
        chartsHandle.openTicker(trade.ticker, start, end)
    }

    private fun onOpenDetails(profileTradeId: ProfileTradeId) {

        tradeContentLauncher.openTrade(profileTradeId)
    }

    private fun onClearMarkedTrades() {
        markedTradeIds.clear()
        chartsHandle.setMarkedTrades(markedTradeIds)
    }

    private fun onApplyFilter(newTradeFilter: TradeFilter) {
        tradeFilter.value = newTradeFilter
    }
}
