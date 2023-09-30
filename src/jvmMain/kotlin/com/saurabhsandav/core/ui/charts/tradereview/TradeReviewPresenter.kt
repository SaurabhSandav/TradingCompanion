package com.saurabhsandav.core.ui.charts.tradereview

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trades.Trade
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.ui.charts.ChartMarkersProvider
import com.saurabhsandav.core.ui.charts.tradereview.model.TradeReviewEvent
import com.saurabhsandav.core.ui.charts.tradereview.model.TradeReviewEvent.*
import com.saurabhsandav.core.ui.charts.tradereview.model.TradeReviewState
import com.saurabhsandav.core.ui.charts.tradereview.model.TradeReviewState.TradeEntry
import com.saurabhsandav.core.ui.charts.tradereview.model.TradeReviewState.TradesByDay
import com.saurabhsandav.core.ui.common.app.AppWindowsManager
import com.saurabhsandav.core.ui.landing.model.LandingState.TradeWindowParams
import com.saurabhsandav.core.utils.PrefKeys
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import java.math.BigDecimal
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Stable
internal class TradeReviewPresenter(
    private val coroutineScope: CoroutineScope,
    private val appModule: AppModule,
    private val onOpenChart: (
        ticker: String,
        start: Instant,
        end: Instant?,
    ) -> Unit,
    private val markersProvider: ChartMarkersProvider,
    private val tradeWindowsManager: AppWindowsManager<TradeWindowParams>,
    private val tradingProfiles: TradingProfiles = appModule.tradingProfiles,
    private val appPrefs: FlowSettings = appModule.appPrefs,
) {

    private val selectedProfileId = appPrefs.getLongOrNullFlow(PrefKeys.TradeReviewTradingProfile)
        .stateIn(coroutineScope, SharingStarted.Eagerly, null)

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule TradeReviewState(
            selectedProfileId = selectedProfileId.collectAsState().value,
            tradesByDays = getTradesByDays().value,
            eventSink = ::onEvent,
        )
    }

    private fun onEvent(event: TradeReviewEvent) {

        when (event) {
            is SelectProfile -> onSelectProfile(event.id)
            is MarkTrade -> onMarkTrade(event.id, event.isMarked)
            is SelectTrade -> onSelectTrade(event.id)
            is OpenDetails -> onOpenDetails(event.id)
        }
    }

    @Composable
    private fun getTradesByDays(): State<ImmutableList<TradesByDay>> {
        return remember {

            selectedProfileId
                .flatMapLatest { id ->
                    if (id != null) tradingProfiles.getProfileOrNull(id) else flowOf(null)
                }
                .filterNotNull()
                .flatMapLatest { profile ->

                    val tradingRecord = tradingProfiles.getRecord(profile.id)

                    tradingRecord.trades.allTrades.combine(markersProvider.markedTradeIds) { trades, markedTradeIds ->
                        trades
                            .groupBy { it.entryTimestamp.date }
                            .map { (date, list) ->

                                TradesByDay(
                                    dayHeader = DateTimeFormatter
                                        .ofLocalizedDate(FormatStyle.LONG)
                                        .format(date.toJavaLocalDate()),
                                    trades = list
                                        .map { it.toTradeListEntry(it.id in markedTradeIds) }
                                        .toImmutableList(),
                                )
                            }.toImmutableList()
                    }
                }
        }.collectAsState(persistentListOf())
    }

    private fun Trade.toTradeListEntry(isMarked: Boolean): TradeEntry {

        val instrumentCapitalized = instrument.strValue
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

        val timeZone = TimeZone.of("Asia/Kolkata")
        val entryInstant = entryTimestamp.toInstant(timeZone)
        val exitInstant = exitTimestamp?.toInstant(timeZone)

        fun formatDuration(duration: Duration): String {

            val durationSeconds = duration.inWholeSeconds

            return "%02d:%02d:%02d".format(
                durationSeconds / 3600,
                (durationSeconds % 3600) / 60,
                durationSeconds % 60,
            )
        }

        val durationStr = when {
            exitInstant != null -> flowOf(formatDuration(exitInstant - entryInstant))
            else -> flow {
                while (true) {
                    emit(formatDuration(Clock.System.now() - entryInstant))
                    delay(1.seconds)
                }
            }
        }

        return TradeEntry(
            isMarked = isMarked,
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
            duration = durationStr,
            pnl = pnl.toPlainString(),
            isProfitable = pnl > BigDecimal.ZERO,
            netPnl = netPnl.toPlainString(),
            isNetProfitable = netPnl > BigDecimal.ZERO,
        )
    }

    private fun onSelectProfile(id: Long) = coroutineScope.launchUnit {

        // Save selected profile
        appPrefs.putLong(PrefKeys.TradeReviewTradingProfile, id)

        // Clear marked trades
        markersProvider.clearMarkedTrades()
    }

    private fun onMarkTrade(id: Long, isMarked: Boolean) {

        when {
            isMarked -> markersProvider.markTrade(id)
            else -> markersProvider.unMarkTrade(id)
        }
    }

    private fun onSelectTrade(id: Long) = coroutineScope.launchUnit {

        // Mark selected trade
        markersProvider.markTrade(id)

        val profileId = selectedProfileId.value ?: error("Trade review profile not set")
        val tradingRecord = tradingProfiles.getRecord(profileId)

        val trade = tradingRecord.trades.getById(id).first()
        val start = trade.entryTimestamp.toInstant(TimeZone.currentSystemDefault())
        val end = trade.exitTimestamp?.toInstant(TimeZone.currentSystemDefault())

        // Show trade on chart
        onOpenChart(trade.ticker, start, end)
    }

    private fun onOpenDetails(id: Long) {

        val profileId = selectedProfileId.value ?: error("Trade review profile not set")

        val window = tradeWindowsManager.windows.find {
            it.params.profileId == profileId && it.params.tradeId == id
        }

        when (window) {

            // Open new window
            null -> {

                val params = TradeWindowParams(
                    profileId = profileId,
                    tradeId = id,
                )

                tradeWindowsManager.newWindow(params)
            }

            // Window already open. Bring to front.
            else -> window.toFront()
        }
    }
}
