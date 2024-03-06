package com.saurabhsandav.core.ui.studies.impl

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.trades.Trade
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.utils.emitInto
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

internal class StatsStudy(
    private val profileId: ProfileId,
    private val tradingProfiles: TradingProfiles,
) : Study {

    @Composable
    override fun render() {

        val generalStats = remember {
            flow {
                tradingProfiles.getRecord(profileId).trades.allTrades.map(::calculateGeneralStats).emitInto(this)
            }
        }.collectAsState(null).value

        Box {

            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier.verticalScroll(scrollState).padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
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
    private fun Stats(generalStats: GeneralStats) {

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Row {

                StatEntry(
                    label = "Pnl",
                    value = generalStats.pnl,
                )

                StatEntry(
                    label = "Net Pnl",
                    value = generalStats.netPnl,
                )

                StatEntry(
                    label = "Total Fees",
                    value = generalStats.totalFees,
                )

                StatEntry(
                    label = "Average Fees",
                    value = generalStats.averageFees,
                )

                StatEntry(
                    label = "Profit Factor",
                    value = generalStats.profitFactor,
                )

                StatEntry(
                    label = "Average Holding Time",
                    value = generalStats.averageHoldingTime,
                )

                StatEntry(
                    label = "Expectancy",
                    value = generalStats.expectancy,
                )
            }

            Row {

                StatEntry(
                    label = "Wins",
                    value = generalStats.wins,
                )

                StatEntry(
                    label = "Win %",
                    value = generalStats.winPercent,
                )

                StatEntry(
                    label = "Largest Win",
                    value = generalStats.largestWin,
                )

                StatEntry(
                    label = "Average Win",
                    value = generalStats.averageWin,
                )

                StatEntry(
                    label = "Longest Win Streak",
                    value = generalStats.longestWinStreak,
                )

                StatEntry(
                    label = "Average Win Holding Time",
                    value = generalStats.averageWinHoldingTime,
                )
            }

            Row {

                StatEntry(
                    label = "Losses",
                    value = generalStats.losses,
                )

                StatEntry(
                    label = "Loss %",
                    value = generalStats.lossPercent,
                )

                StatEntry(
                    label = "Largest Loss",
                    value = generalStats.largestLoss,
                )

                StatEntry(
                    label = "Average Loss",
                    value = generalStats.averageLoss,
                )

                StatEntry(
                    label = "Longest Loss Streak",
                    value = generalStats.longestLossStreak,
                )

                StatEntry(
                    label = "Average Loss Holding Time",
                    value = generalStats.averageLossHoldingTime,
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
            modifier = Modifier.width(IntrinsicSize.Min).border(1.dp, Color.Gray).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {

            Text(label, Modifier.width(IntrinsicSize.Max))

            Text(
                text = value,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }

    private fun calculateGeneralStats(trades: List<Trade>): GeneralStats? {

        val allTrades = trades.filter { it.isClosed }

        if (allTrades.isEmpty()) return null

        fun longestStreak(trades: List<Trade>, predicate: (Trade) -> Boolean): Int {

            var currentStreak = 0
            var longestStreak = 0

            for (element in trades) {
                when {
                    predicate(element) -> currentStreak++
                    else -> {
                        longestStreak = maxOf(longestStreak, currentStreak)
                        currentStreak = 0
                    }
                }
            }

            return maxOf(longestStreak, currentStreak)
        }

        fun List<BigDecimal>.average() = when {
            this.isEmpty() -> BigDecimal.ZERO
            else -> reduce { acc, value -> acc + value } / size.toBigDecimal()
        }

        val totalTrades = allTrades.size
        val totalFees = allTrades.sumOf { it.fees }
        val averageFees = totalFees / totalTrades.toBigDecimal()

        val averageHoldingTime = run {

            val holdingTimes = allTrades.map { it.exitTimestamp!! - it.entryTimestamp }

            val averageHoldingTimeSeconds = holdingTimes.sumOf { it.inWholeSeconds } / totalTrades
            averageHoldingTimeSeconds / 60
        }

        val winTrades = allTrades.filter { it.pnl > BigDecimal.ZERO }
        val winCount = winTrades.count()
        val winDecimal = winCount / totalTrades.toFloat()
        val winTotalPnl = winTrades.sumOf { it.pnl }
        val averageWin = winTrades.map { it.pnl }.average()
        val longestWinStreak = longestStreak(allTrades) { it.pnl > BigDecimal.ZERO }

        val averageWinHoldingTime = run {

            val holdingTimes = winTrades.map { it.exitTimestamp!! - it.entryTimestamp }

            val averageHoldingTimeSeconds = holdingTimes.map { it.inWholeSeconds }.average()
            averageHoldingTimeSeconds / 60
        }

        val lossTrades = allTrades.filter { it.pnl <= BigDecimal.ZERO }
        val lossCount = lossTrades.count()
        val lossDecimal = lossCount / totalTrades.toFloat()
        val lossTotalPnl = lossTrades.sumOf { it.pnl }
        val averageLoss = lossTrades.map { it.pnl }.average()
        val longestLossStreak = longestStreak(allTrades) { it.pnl <= BigDecimal.ZERO }

        val averageLossHoldingTime = run {

            val holdingTimes = lossTrades.map { it.exitTimestamp!! - it.entryTimestamp }

            val averageHoldingTimeSeconds = holdingTimes.map { it.inWholeSeconds }.average()
            averageHoldingTimeSeconds / 60
        }

        val expectancy = (winDecimal.toBigDecimal() * averageWin) + (lossDecimal.toBigDecimal() * averageLoss)

        return GeneralStats(
            pnl = allTrades.sumOf { it.pnl }.toPlainString(),
            netPnl = allTrades.sumOf { it.netPnl }.toPlainString(),
            totalFees = totalFees.toPlainString(),
            averageFees = averageFees.toPlainString(),
            profitFactor = (winTotalPnl / lossTotalPnl).toPlainString(),
            averageHoldingTime = "$averageHoldingTime minutes",
            expectancy = "%.2f".format(expectancy),
            wins = winCount.toString(),
            winPercent = (winDecimal * 100).toString(),
            largestWin = winTrades.maxOfOrNull { it.pnl }?.toString() ?: "",
            averageWin = averageWin.toPlainString(),
            longestWinStreak = longestWinStreak.toString(),
            averageWinHoldingTime = "${"%.2f".format(averageWinHoldingTime)} minutes",
            losses = lossCount.toString(),
            lossPercent = (lossDecimal * 100).toString(),
            largestLoss = lossTrades.minOfOrNull { it.pnl }?.toString() ?: "",
            averageLoss = averageLoss.toPlainString(),
            longestLossStreak = longestLossStreak.toString(),
            averageLossHoldingTime = "${"%.2f".format(averageLossHoldingTime)} minutes",
        )
    }

    data class GeneralStats(
        val pnl: String = "",
        val netPnl: String = "",
        val totalFees: String = "",
        val averageFees: String = "",
        val profitFactor: String = "",
        val averageHoldingTime: String = "",
        val expectancy: String = "",
        val wins: String = "",
        val winPercent: String = "",
        val largestWin: String = "",
        val averageWin: String = "",
        val longestWinStreak: String = "",
        val averageWinHoldingTime: String = "",
        val losses: String = "",
        val lossPercent: String = "",
        val largestLoss: String = "",
        val averageLoss: String = "",
        val longestLossStreak: String = "",
        val averageLossHoldingTime: String = "",
    )

    class Factory(
        private val profileId: ProfileId,
        private val tradingProfiles: TradingProfiles,
    ) : Study.Factory<StatsStudy> {

        override val name: String = "Stats"

        override fun create() = StatsStudy(profileId, tradingProfiles)
    }
}
