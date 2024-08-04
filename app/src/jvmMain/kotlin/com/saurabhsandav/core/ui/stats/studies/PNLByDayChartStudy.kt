package com.saurabhsandav.core.ui.stats.studies

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.brokerageAtExit
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.common.chart.ChartPage
import com.saurabhsandav.core.ui.common.chart.arrangement.ChartArrangement
import com.saurabhsandav.core.ui.common.chart.arrangement.single
import com.saurabhsandav.core.ui.common.chart.crosshairMove
import com.saurabhsandav.core.ui.common.chart.state.ChartPageState
import com.saurabhsandav.core.ui.common.chart.themedChartOptions
import com.saurabhsandav.core.ui.common.webview.WebViewState
import com.saurabhsandav.core.utils.emitInto
import com.saurabhsandav.lightweight_charts.baselineSeries
import com.saurabhsandav.lightweight_charts.data.BaselineData
import com.saurabhsandav.lightweight_charts.data.Time
import com.saurabhsandav.lightweight_charts.options.ChartOptions.CrosshairOptions
import com.saurabhsandav.lightweight_charts.options.ChartOptions.CrosshairOptions.CrosshairMode
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
    private val webViewStateProvider: () -> WebViewState,
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
        val chartPageState = remember {
            ChartPageState(
                coroutineScope = coroutineScope,
                arrangement = arrangement,
                webViewState = webViewStateProvider(),
            )
        }
        val chart = remember {
            arrangement.newChart(options = themedOptions.copy(crosshair = CrosshairOptions(mode = CrosshairMode.Normal)))
        }

        ChartPage(chartPageState)

        LaunchedEffect(chart) {

            // Show chart
            chartPageState.connect(chart)

            val baselineSeries by chart.baselineSeries()

            // Update Legend
            chart.crosshairMove().onEach { params ->

                val value = baselineSeries.getMouseEventDataFrom(params.seriesData)
                    ?.let { it as? BaselineData.Item }
                    ?.value
                    ?.toString()
                    .orEmpty()

                arrangement.setLegend(listOf("PNL $value"))
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
        private val webViewStateProvider: () -> WebViewState,
    ) : Study.Factory<PNLByDayChartStudy> {

        override val name: String = "PNL By Day (Chart)"

        override fun create() = PNLByDayChartStudy(profileId, tradingProfiles, webViewStateProvider)
    }
}
