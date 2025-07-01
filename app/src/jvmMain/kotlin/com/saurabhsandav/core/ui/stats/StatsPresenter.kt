package com.saurabhsandav.core.ui.stats

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.trading.ProfileId
import com.saurabhsandav.core.trading.TradingProfiles
import com.saurabhsandav.core.ui.common.app.AppWindowsManager
import com.saurabhsandav.core.ui.stats.model.StatsEvent
import com.saurabhsandav.core.ui.stats.model.StatsState
import com.saurabhsandav.core.ui.stats.model.StatsState.StatEntry
import com.saurabhsandav.core.ui.stats.model.StatsState.StatsCategory
import com.saurabhsandav.core.ui.stats.studies.Study
import com.saurabhsandav.core.utils.emitInto
import com.saurabhsandav.trading.record.stats.TradingStats
import com.saurabhsandav.trading.record.stats.buildStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration

internal class StatsPresenter(
    coroutineScope: CoroutineScope,
    private val profileId: ProfileId,
    private val tradingProfiles: TradingProfiles,
    private val studyFactories: List<Study.Factory<out Study>>,
) {

    val studyWindowsManager = AppWindowsManager<Study.Factory<*>>()

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule StatsState(
            statsCategories = getStatsCategories(),
            studyFactories = studyFactories,
            studyWindowsManager = studyWindowsManager,
            eventSink = ::onEvent,
        )
    }

    private fun onEvent(event: StatsEvent) {

        when (event) {
            is StatsEvent.OpenStudy -> onOpenStudy(event.studyFactory)
        }
    }

    @Composable
    private fun getStatsCategories(): List<StatsCategory>? {
        return produceState<List<StatsCategory>?>(null) {

            flow {
                tradingProfiles.getRecord(profileId)
                    .buildStats()
                    .map { stats -> stats?.let(::generateStatEntryList) }
                    .emitInto(this)
            }.collect { stats -> value = stats }
        }.value
    }

    private fun generateStatEntryList(stats: TradingStats): List<StatsCategory> {
        return listOf(
            StatsCategory(
                label = "All",
                entries = listOf(
                    StatEntry(label = "Count", value = stats.count.toString()),
                    StatEntry(label = "PNL", value = stats.pnl.toPlainString()),
                    StatEntry(label = "Net PNL", value = stats.pnlNet.toPlainString()),
                    StatEntry(label = "Peak PNL", value = stats.pnlPeak.toPlainString()),
                    StatEntry(label = "Peak Net PNL", value = stats.pnlNetPeak.toPlainString()),
                    StatEntry(label = "Fees", value = stats.fees.toPlainString()),
                    StatEntry(label = "Average Fees", value = stats.feesAverage.toPlainString()),
                    StatEntry(label = "Profit Factor", value = stats.profitFactor?.toPlainString().orEmpty()),
                    StatEntry(label = "Average Duration", value = stats.durationAverage.displayStr()),
                    StatEntry(label = "Expectancy", value = stats.expectancy?.toPlainString().orEmpty()),
                    StatEntry(label = "Drawdowns", value = stats.drawdowns.size.toString()),
                    StatEntry(label = "Max Drawdown", value = stats.drawdownMax?.toPlainString().orEmpty()),
                    StatEntry(label = "Average Drawdown", value = stats.drawdownAverage?.toPlainString().orEmpty()),
                    StatEntry(
                        label = "Max Drawdown Duration",
                        value = stats.drawdownDurationMax?.displayStr().orEmpty(),
                    ),
                    StatEntry(
                        label = "Average Drawdown Duration",
                        value = stats.drawdownDurationAverage?.displayStr().orEmpty(),
                    ),
                ),
            ),
            StatsCategory(
                label = "Wins",
                entries = listOf(
                    StatEntry(label = "Count", value = stats.winCount.toString()),
                    StatEntry(label = "PNL", value = stats.winPnl.toPlainString()),
                    StatEntry(label = "Net PNL", value = stats.winPnlNet.toPlainString()),
                    StatEntry(label = "Fees", value = stats.winFees.toPlainString()),
                    StatEntry(label = "Percent", value = stats.winPercent.toPlainString()),
                    StatEntry(label = "Largest", value = stats.winLargest?.toPlainString().orEmpty()),
                    StatEntry(label = "Average", value = stats.winAverage?.toPlainString().orEmpty()),
                    StatEntry(label = "Longest Streak", value = stats.winStreakLongest.toString()),
                    StatEntry(label = "Average Duration", value = stats.winDurationAverage?.displayStr().orEmpty()),
                ),
            ),
            StatsCategory(
                label = "Losses",
                entries = listOf(
                    StatEntry(label = "Count", value = stats.lossCount.toString()),
                    StatEntry(label = "PNL", value = stats.lossPnl.toPlainString()),
                    StatEntry(label = "Net PNL", value = stats.lossPnlNet.toPlainString()),
                    StatEntry(label = "Fees", value = stats.lossFees.toPlainString()),
                    StatEntry(label = "Percent", value = stats.lossPercent.toPlainString()),
                    StatEntry(label = "Largest", value = stats.lossLargest?.toPlainString().orEmpty()),
                    StatEntry(label = "Average", value = stats.lossAverage?.toPlainString().orEmpty()),
                    StatEntry(label = "Longest Streak", value = stats.lossStreakLongest.toString()),
                    StatEntry(label = "Average Duration", value = stats.lossDurationAverage?.displayStr().orEmpty()),
                ),
            ),
        )
    }

    private fun onOpenStudy(studyFactory: Study.Factory<*>) {

        val window = studyWindowsManager.windows.find { it.params == studyFactory }

        when (window) {

            // Open new window
            null -> studyWindowsManager.newWindow(studyFactory)

            // Window already open. Bring to front.
            else -> window.toFront()
        }
    }

    private fun Duration.displayStr(): String {

        val seconds = inWholeSeconds

        if (seconds == 0L) return "0 seconds"

        val days = seconds / 86400
        val remainingSeconds = seconds % 86400
        val hours = remainingSeconds / 3600
        val minutes = (remainingSeconds % 3600) / 60
        val remainingSecondsLeft = remainingSeconds % 60

        return buildString {
            if (days > 0) append("$days ${if (days == 1L) "day" else "days"} ")
            if (hours > 0) append("$hours ${if (hours == 1L) "hour" else "hours"} ")
            if (minutes > 0) append("$minutes ${if (minutes == 1L) "minute" else "minutes"} ")
            if (isEmpty() || remainingSecondsLeft > 0) {
                append("$remainingSecondsLeft ${if (remainingSecondsLeft == 1L) "second" else "seconds"}")
            }
        }.trim()
    }
}
