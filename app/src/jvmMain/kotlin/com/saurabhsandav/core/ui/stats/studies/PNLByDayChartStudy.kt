package com.saurabhsandav.core.ui.stats.studies

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.brokerageAtExit
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.common.chart.ChartPage
import com.saurabhsandav.core.ui.common.chart.arrangement.ChartArrangement
import com.saurabhsandav.core.ui.common.chart.arrangement.single
import com.saurabhsandav.core.ui.common.chart.crosshairMove
import com.saurabhsandav.core.ui.common.chart.legend.LegendItem
import com.saurabhsandav.core.ui.common.chart.state.ChartPageState
import com.saurabhsandav.core.ui.common.chart.themedChartOptions
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.common.webview.WebViewState
import com.saurabhsandav.core.utils.emitInto
import com.saurabhsandav.lightweight_charts.baselineSeries
import com.saurabhsandav.lightweight_charts.data.BaselineData
import com.saurabhsandav.lightweight_charts.data.Time
import com.saurabhsandav.lightweight_charts.options.ChartOptions.CrosshairOptions
import com.saurabhsandav.lightweight_charts.options.ChartOptions.CrosshairOptions.CrosshairMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal

internal class PNLByDayChartStudy(
    profileId: ProfileId,
    tradingProfiles: TradingProfiles,
    private val webViewStateProvider: (CoroutineScope) -> WebViewState,
) : Study {

    private val data = flow {
        tradingProfiles
            .getRecord(profileId)
            .trades
            .allTrades
            .map { trades ->
                trades.filter { it.isClosed }
                    .asReversed()
                    .groupingBy { trade ->
                        trade.entryTimestamp.toLocalDateTime(TimeZone.currentSystemDefault()).date
                    }
                    .fold(
                        initialValueSelector = { _, _ -> BigDecimal.ZERO },
                        operation = { _, accumulator, trade -> accumulator + trade.brokerageAtExit()!!.netPNL },
                    )
                    .map { (localDate, bigDecimal) ->
                        BaselineData.Item(
                            time = Time.BusinessDay(
                                year = localDate.year,
                                month = localDate.monthNumber,
                                day = localDate.dayOfMonth,
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
        val arrangement = remember { ChartArrangement.single() }
        val chartPageState = remember(coroutineScope) {
            ChartPageState(
                coroutineScope = coroutineScope,
                arrangement = arrangement,
                webViewState = webViewStateProvider(coroutineScope),
            )
        }
        val chart = remember {
            arrangement.newChart(options = themedOptions.copy(crosshair = CrosshairOptions(mode = CrosshairMode.Normal)))
        }

        var legendValue by state { "" }

        ChartPage(chartPageState) {

            LegendItem(
                label = { Text("PNL") },
                values = { Text(legendValue) },
            )
        }

        LaunchedEffect(chart) {

            // Show chart
            chartPageState.connect(chart)

            try {
                awaitCancellation()
            } finally {
                chartPageState.disconnect(chart)
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
