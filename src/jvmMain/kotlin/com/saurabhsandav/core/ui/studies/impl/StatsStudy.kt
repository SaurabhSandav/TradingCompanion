package com.saurabhsandav.core.ui.studies.impl

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.stats.TradingStats
import com.saurabhsandav.core.trades.stats.buildStats
import com.saurabhsandav.core.ui.theme.dimens
import com.saurabhsandav.core.utils.emitInto
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration

internal class StatsStudy(
    private val profileId: ProfileId,
    private val tradingProfiles: TradingProfiles,
) : Study {

    @Composable
    override fun render() {

        val generalStats = remember {
            flow {
                tradingProfiles.getRecord(profileId)
                    .buildStats()
                    .map { stats -> stats?.let(::toDisplayableStats) }
                    .emitInto(this)
            }
        }.collectAsState(null).value

        Box {

            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier.verticalScroll(scrollState).padding(MaterialTheme.dimens.containerPadding),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.columnVerticalSpacing),
            ) {

                when {
                    generalStats != null -> Stats(generalStats)
                    else -> Text("No trades")
                }
            }

            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                adapter = rememberScrollbarAdapter(scrollState)
            )
        }
    }

    @Composable
    private fun Stats(displayableStats: DisplayableStats) {

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.columnVerticalSpacing),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Row {

                StatEntry(
                    label = "Pnl",
                    value = displayableStats.pnl,
                )

                StatEntry(
                    label = "Net Pnl",
                    value = displayableStats.pnlNet,
                )

                StatEntry(
                    label = "Total Fees",
                    value = displayableStats.feesTotal,
                )

                StatEntry(
                    label = "Average Fees",
                    value = displayableStats.feesAverage,
                )

                StatEntry(
                    label = "Profit Factor",
                    value = displayableStats.profitFactor,
                )

                StatEntry(
                    label = "Average Holding Time",
                    value = displayableStats.durationAverage,
                )

                StatEntry(
                    label = "Expectancy",
                    value = displayableStats.expectancy,
                )
            }

            Row {

                StatEntry(
                    label = "Wins",
                    value = displayableStats.winCount,
                )

                StatEntry(
                    label = "Win %",
                    value = displayableStats.winPercent,
                )

                StatEntry(
                    label = "Largest Win",
                    value = displayableStats.winLargest,
                )

                StatEntry(
                    label = "Average Win",
                    value = displayableStats.winAverage,
                )

                StatEntry(
                    label = "Longest Win Streak",
                    value = displayableStats.winStreakLongest,
                )

                StatEntry(
                    label = "Average Win Holding Time",
                    value = displayableStats.winDurationAverage,
                )
            }

            Row {

                StatEntry(
                    label = "Losses",
                    value = displayableStats.lossCount,
                )

                StatEntry(
                    label = "Loss %",
                    value = displayableStats.lossPercent,
                )

                StatEntry(
                    label = "Largest Loss",
                    value = displayableStats.lossLargest,
                )

                StatEntry(
                    label = "Average Loss",
                    value = displayableStats.lossAverage,
                )

                StatEntry(
                    label = "Longest Loss Streak",
                    value = displayableStats.lossStreakLongest,
                )

                StatEntry(
                    label = "Average Loss Holding Time",
                    value = displayableStats.lossDurationAverage,
                )
            }
        }
    }

    @Composable
    private fun StatEntry(
        label: String,
        value: String,
    ) {

        Column(
            modifier = Modifier.width(IntrinsicSize.Min)
                .border(1.dp, Color.Gray)
                .padding(MaterialTheme.dimens.containerPadding),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.columnVerticalSpacing),
        ) {

            Text(label, Modifier.width(IntrinsicSize.Max))

            Text(
                text = value,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }

    private fun toDisplayableStats(stats: TradingStats) = DisplayableStats(
        pnl = stats.pnl.toPlainString(),
        pnlNet = stats.pnlNet.toPlainString(),
        feesTotal = stats.fees.toPlainString(),
        feesAverage = stats.feesAverage.toPlainString(),
        profitFactor = stats.profitFactor?.toPlainString().orEmpty(),
        durationAverage = stats.durationAverage.displayStr(),
        expectancy = "%.2f".format(stats.expectancy),
        winCount = stats.winCount.toString(),
        winPercent = stats.winPercent.toPlainString(),
        winLargest = stats.winLargest?.toPlainString().orEmpty(),
        winAverage = stats.winAverage?.toPlainString().orEmpty(),
        winStreakLongest = stats.winStreakLongest.toString(),
        winDurationAverage = stats.winDurationAverage?.displayStr().orEmpty(),
        lossCount = stats.lossCount.toString(),
        lossPercent = stats.lossPercent.toString(),
        lossLargest = stats.lossLargest?.toPlainString().orEmpty(),
        lossAverage = stats.lossAverage?.toPlainString().orEmpty(),
        lossStreakLongest = stats.lossStreakLongest.toString(),
        lossDurationAverage = stats.lossDurationAverage?.displayStr().orEmpty(),
    )

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
            if (isEmpty() || remainingSecondsLeft > 0)
                append("$remainingSecondsLeft ${if (remainingSecondsLeft == 1L) "second" else "seconds"}")
        }.trim()
    }

    data class DisplayableStats(
        val pnl: String = "",
        val pnlNet: String = "",
        val feesTotal: String = "",
        val feesAverage: String = "",
        val profitFactor: String = "",
        val durationAverage: String = "",
        val expectancy: String = "",
        val winCount: String = "",
        val winPercent: String = "",
        val winLargest: String = "",
        val winAverage: String = "",
        val winStreakLongest: String = "",
        val winDurationAverage: String = "",
        val lossCount: String = "",
        val lossPercent: String = "",
        val lossLargest: String = "",
        val lossAverage: String = "",
        val lossStreakLongest: String = "",
        val lossDurationAverage: String = "",
    )

    class Factory(
        private val profileId: ProfileId,
        private val tradingProfiles: TradingProfiles,
    ) : Study.Factory<StatsStudy> {

        override val name: String = "Stats"

        override fun create() = StatsStudy(profileId, tradingProfiles)
    }
}
