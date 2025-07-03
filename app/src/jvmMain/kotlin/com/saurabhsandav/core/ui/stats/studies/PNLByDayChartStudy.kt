package com.saurabhsandav.core.ui.stats.studies

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.saurabhsandav.core.trading.ProfileId
import com.saurabhsandav.core.trading.TradingProfiles
import com.saurabhsandav.core.ui.common.chart.SimpleChart
import com.saurabhsandav.core.ui.common.chart.crosshairMove
import com.saurabhsandav.core.ui.common.chart.legend.LegendItem
import com.saurabhsandav.core.ui.common.chart.state.ChartPageState
import com.saurabhsandav.core.ui.common.chart.themedChartOptions
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.common.webview.WebViewState
import com.saurabhsandav.core.utils.emitInto
import com.saurabhsandav.lightweightcharts.baselineSeries
import com.saurabhsandav.lightweightcharts.data.BaselineData
import com.saurabhsandav.lightweightcharts.data.Time
import com.saurabhsandav.lightweightcharts.options.ChartOptions.CrosshairOptions
import com.saurabhsandav.lightweightcharts.options.ChartOptions.CrosshairOptions.CrosshairMode
import com.saurabhsandav.lightweightcharts.options.TimeScaleOptions
import com.saurabhsandav.trading.record.brokerageAtExit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal

internal class PNLByDayChartStudy(
    profileId: ProfileId,
    tradingProfiles: TradingProfiles,
    private val webViewStateProvider: (CoroutineScope) -> WebViewState,
) : Study {

    private val data = flow {

        val tradingRecord = tradingProfiles.getRecord(profileId)

        tradingRecord
            .trades
            .allTrades
            .map { trades ->
                trades.filter { it.isClosed }
                    .asReversed()
                    .groupingBy { trade ->
                        trade.entryTimestamp.toLocalDateTime(TimeZone.currentSystemDefault()).date
                    }
                    .fold(BigDecimal.ZERO) { accumulator, trade ->
                        val broker = tradingRecord.brokerProvider.getBroker(trade.brokerId)
                        accumulator + trade.brokerageAtExit(broker)!!.netPNL
                    }
                    .map { (localDate, bigDecimal) ->
                        BaselineData.Item(
                            time = Time.BusinessDay(
                                year = localDate.year,
                                month = localDate.month.number,
                                day = localDate.day,
                            ),
                            value = bigDecimal.toDouble(),
                        )
                    }
            }
            .emitInto(this)
    }

    @Composable
    override fun render() {

        val themedOptions = themedChartOptions()
        val coroutineScope = rememberCoroutineScope()
        val pageState = remember(coroutineScope) {
            ChartPageState(
                coroutineScope = coroutineScope,
                webViewState = webViewStateProvider(coroutineScope),
            )
        }
        val chart = remember {
            pageState.addChart(
                options = themedOptions.copy(
                    crosshair = CrosshairOptions(mode = CrosshairMode.Normal),
                    timeScale = TimeScaleOptions(lockVisibleTimeRangeOnResize = true),
                ),
            )
        }

        var legendValue by state { "" }

        SimpleChart(
            pageState = pageState,
            legend = {

                LegendItem(
                    label = { Text("PNL") },
                    values = { Text(legendValue) },
                )
            },
        )

        LaunchedEffect(chart) {

            // Show chart
            pageState.showChart(chart.id)

            try {
                awaitCancellation()
            } finally {
                pageState.removeChart(chart.id)
            }
        }

        LaunchedEffect(chart) {

            val baselineSeries by chart.baselineSeries()

            // Update Legend
            chart.crosshairMove().onEach { params ->

                legendValue = baselineSeries.getMouseEventDataFrom(params.seriesData)
                    ?.let { it as? BaselineData.Item }
                    ?.value
                    ?.toString()
                    .orEmpty()
            }.launchIn(this)

            // Set Data
            data.collect {
                baselineSeries.setData(it)
                chart.timeScale.fitContent()
            }
        }

        LaunchedEffect(themedOptions) {
            chart.applyOptions(themedOptions)
        }
    }

    class Factory(
        private val profileId: ProfileId,
        private val tradingProfiles: TradingProfiles,
        private val webViewStateProvider: (CoroutineScope) -> WebViewState,
    ) : Study.Factory<PNLByDayChartStudy> {

        override val name: String = "PNL By Day (Chart)"

        override fun create() = PNLByDayChartStudy(profileId, tradingProfiles, webViewStateProvider)
    }
}
