package com.saurabhsandav.core.ui.stats.studies

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.saurabhsandav.core.chart.baselineSeries
import com.saurabhsandav.core.chart.data.LineData
import com.saurabhsandav.core.chart.data.SingleValueData
import com.saurabhsandav.core.chart.data.Time
import com.saurabhsandav.core.chart.options.ChartOptions.CrosshairOptions
import com.saurabhsandav.core.chart.options.ChartOptions.CrosshairOptions.CrosshairMode
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal

internal class PNLByMonthChartStudy(
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

                        val ldt = trade.entryTimestamp.toLocalDateTime(TimeZone.currentSystemDefault())

                        LocalDate(
                            year = ldt.year,
                            monthNumber = ldt.monthNumber,
                            dayOfMonth = 1,
                        )
                    }
                    .fold(
                        initialValueSelector = { _, _ -> BigDecimal.ZERO },
                        operation = { _, accumulator, trade -> accumulator + trade.brokerageAtExit()!!.netPNL },
                    )
                    .map { (localDate, bigDecimal) ->
                        LineData(
                            time = Time.BusinessDay(
                                year = localDate.year,
                                month = localDate.monthNumber,
                                day = localDate.dayOfMonth,
                            ),
                            value = bigDecimal,
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
                val value = (params.seriesData[baselineSeries] as? SingleValueData?)?.value?.toString().orEmpty()
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
    ) : Study.Factory<PNLByMonthChartStudy> {

        override val name: String = "PNL By Month (Chart)"

        override fun create() = PNLByMonthChartStudy(profileId, tradingProfiles, webViewStateProvider)
    }
}
