package com.saurabhsandav.core.ui.tradereview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.toMutableStateList
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.di.AppPrefs
import com.saurabhsandav.core.trading.ProfileId
import com.saurabhsandav.core.trading.TradingProfiles
import com.saurabhsandav.core.ui.charts.ChartsHandle
import com.saurabhsandav.core.ui.common.TradeDateTimeFormat
import com.saurabhsandav.core.ui.common.getBrokerTitle
import com.saurabhsandav.core.ui.common.getSymbolTitle
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import com.saurabhsandav.core.ui.tradecontent.TradeContentLauncher
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewEvent
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewEvent.ApplyFilter
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewEvent.ClearMarkedTrades
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewEvent.MarkAllTrades
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewEvent.MarkTrade
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewEvent.OpenDetails
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewEvent.ProfileSelected
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewEvent.SelectTrade
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewState
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewState.MarkedTradeItem
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewState.TradeItem
import com.saurabhsandav.core.utils.PrefKeys
import com.saurabhsandav.core.utils.emitInto
import com.saurabhsandav.core.utils.launchUnit
import com.saurabhsandav.core.utils.mapList
import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.trading.record.TradeDisplay
import com.saurabhsandav.trading.record.model.TradeFilter
import com.saurabhsandav.trading.record.model.TradeSort
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@AssistedInject
internal class TradeReviewPresenter(
    @Assisted private val coroutineScope: CoroutineScope,
    @Assisted private val chartsHandle: ChartsHandle,
    private val tradeContentLauncher: TradeContentLauncher,
    private val tradingProfiles: TradingProfiles,
    @AppPrefs appPrefs: FlowSettings,
) {

    private val selectedProfileId = MutableStateFlow<ProfileId?>(null)

    private val markedTradeIds = chartsHandle.markedTradeIds.toMutableStateList()
    private val tradeFilter = MutableStateFlow(TradeFilter())

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule TradeReviewState(
            selectedProfileId = selectedProfileId.collectAsState().value,
            selectedProfileName = getSelectedProfileName(),
            trades = getTrades(),
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
            MarkAllTrades -> onMarkAllTrades()
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
    private fun getTrades(): Flow<PagingData<TradeItem>> = remember {
        flow {
            coroutineScope {

                val pagingConfig = PagingConfig(
                    pageSize = 70,
                    enablePlaceholders = false,
                    maxSize = 300,
                )

                selectedProfileId
                    .flatMapLatest { id ->
                        if (id != null) tradingProfiles.getProfileOrNull(id) else flowOf(null)
                    }
                    .flatMapLatest { profile ->

                        if (profile == null) return@flatMapLatest flowOf(PagingData.empty<TradeItem>())

                        val trades = tradingProfiles.getRecord(profile.id).trades

                        tradeFilter.flatMapLatest { tradeFilter ->

                            Pager(
                                config = pagingConfig,
                                pagingSourceFactory = {

                                    trades.getDisplayFilteredPagingSource(
                                        filter = tradeFilter,
                                        sort = TradeSort.EntryDesc,
                                    )
                                },
                            ).flow
                                .cachedIn(this)
                                .combine(
                                    snapshotFlow {
                                        markedTradeIds.toList()
                                    },
                                ) { pagingData, markedProfileTradeIds ->

                                    pagingData.map { trade ->

                                        val profileTradeId = ProfileTradeId(
                                            profileId = profile.id,
                                            tradeId = trade.id,
                                        )

                                        trade.toTradeItem(
                                            profileTradeId = profileTradeId,
                                            isMarked = profileTradeId in markedProfileTradeIds,
                                        )
                                    }
                                }
                        }
                    }.emitInto(this@flow)
            }
        }
    }

    private fun TradeDisplay.toTradeItem(
        profileTradeId: ProfileTradeId,
        isMarked: Boolean,
    ): TradeItem {

        fun formatDuration(duration: Duration): String {

            val durationSeconds = duration.inWholeSeconds

            return "%02d:%02d:%02d".format(
                durationSeconds / 3600,
                (durationSeconds % 3600) / 60,
                durationSeconds % 60,
            )
        }

        val duration = when {
            isClosed -> TradeReviewState.Duration.Closed(
                str = formatDuration(exitTimestamp!! - entryTimestamp),
            )

            else -> TradeReviewState.Duration.Open(
                flow = flow {
                    while (true) {
                        emit(formatDuration(Clock.System.now() - entryTimestamp))
                        delay(1.seconds)
                    }
                },
            )
        }

        return TradeItem(
            profileTradeId = profileTradeId,
            isMarked = isMarked,
            broker = getBrokerTitle(),
            ticker = getSymbolTitle(),
            side = side.toString().uppercase(),
            quantity = when {
                !isClosed -> "$closedQuantity / $quantity"
                else -> quantity.toString()
            },
            entry = averageEntry.toString(),
            exit = averageExit?.toString() ?: "",
            entryTime = entryTimestamp.toLocalDateTime(TimeZone.currentSystemDefault()).format(TradeDateTimeFormat),
            duration = duration,
            pnl = pnl.toString(),
            isProfitable = pnl > KBigDecimal.Zero,
            netPnl = netPnl.toString(),
            isNetProfitable = netPnl > KBigDecimal.Zero,
        )
    }

    @Composable
    private fun getMarkedTrades(): State<List<MarkedTradeItem>?> {
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
                                    .getDisplayByIds(ids = tradeIds)
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
        }.collectAsState(null)
    }

    private fun TradeDisplay.toMarkedTradeItem(
        profileId: ProfileId,
        profileName: String,
    ): MarkedTradeItem {

        fun formatDuration(duration: Duration): String {

            val durationSeconds = duration.inWholeSeconds

            return "%02d:%02d:%02d".format(
                durationSeconds / 3600,
                (durationSeconds % 3600) / 60,
                durationSeconds % 60,
            )
        }

        val duration = when {
            isClosed -> TradeReviewState.Duration.Closed(
                str = formatDuration(exitTimestamp!! - entryTimestamp),
            )

            else -> TradeReviewState.Duration.Open(
                flow = flow {
                    while (true) {
                        emit(formatDuration(Clock.System.now() - entryTimestamp))
                        delay(1.seconds)
                    }
                },
            )
        }

        return MarkedTradeItem(
            profileTradeId = ProfileTradeId(profileId = profileId, tradeId = id),
            profileName = profileName,
            broker = getBrokerTitle(),
            ticker = getSymbolTitle(),
            side = side.toString().uppercase(),
            quantity = when {
                !isClosed -> "$closedQuantity / $quantity"
                else -> quantity.toString()
            },
            entry = averageEntry.toString(),
            exit = averageExit?.toString() ?: "",
            entryTime = entryTimestamp.toLocalDateTime(TimeZone.currentSystemDefault()).format(TradeDateTimeFormat),
            duration = duration,
            pnl = pnl.toString(),
            isProfitable = pnl > KBigDecimal.Zero,
            netPnl = netPnl.toString(),
            isNetProfitable = netPnl > KBigDecimal.Zero,
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
        chartsHandle.openSymbol(trade.symbolId, start, end)
    }

    private fun onOpenDetails(profileTradeId: ProfileTradeId) {

        tradeContentLauncher.openTrade(profileTradeId)
    }

    private fun onMarkAllTrades() = coroutineScope.launchUnit {

        val profileId = selectedProfileId.value ?: return@launchUnit
        val trades = tradingProfiles.getRecord(profileId).trades
        val tradesToMark = trades.getFiltered(tradeFilter.value)
            .first()
            .map { ProfileTradeId(profileId, it.id) }

        Snapshot.withMutableSnapshot {
            markedTradeIds.clear()
            markedTradeIds.addAll(tradesToMark)
        }

        chartsHandle.setMarkedTrades(markedTradeIds)
    }

    private fun onClearMarkedTrades() {
        markedTradeIds.clear()
        chartsHandle.setMarkedTrades(markedTradeIds)
    }

    private fun onApplyFilter(newTradeFilter: TradeFilter) {
        tradeFilter.value = newTradeFilter
    }

    @AssistedFactory
    fun interface Factory {

        fun create(
            coroutineScope: CoroutineScope,
            chartsHandle: ChartsHandle,
        ): TradeReviewPresenter
    }
}
