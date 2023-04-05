package com.saurabhsandav.core.ui.studies.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.chart.baselineSeries
import com.saurabhsandav.core.chart.data.LineData
import com.saurabhsandav.core.chart.data.SingleValueData
import com.saurabhsandav.core.chart.data.Time
import com.saurabhsandav.core.chart.options.CrosshairMode
import com.saurabhsandav.core.chart.options.CrosshairOptions
import com.saurabhsandav.core.ui.common.chart.ChartPage
import com.saurabhsandav.core.ui.common.chart.arrangement.ChartArrangement
import com.saurabhsandav.core.ui.common.chart.arrangement.single
import com.saurabhsandav.core.ui.common.chart.crosshairMove
import com.saurabhsandav.core.ui.common.chart.state.ChartPageState
import com.saurabhsandav.core.ui.common.chart.themedChartOptions
import com.saurabhsandav.core.utils.brokerage
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.LocalDate
import java.math.BigDecimal

internal class PNLByMonthChartStudy(
    appModule: AppModule,
) : Study {

    private val data = appModule.tradesRepo
        .allTrades
        .map { trades ->
            trades.filter { it.isClosed }.map { trade ->

                trade.entryTimestamp.date to brokerage(
                    broker = trade.broker,
                    instrument = trade.instrument,
                    entry = trade.averageEntry,
                    exit = trade.averageExit!!,
                    quantity = trade.quantity,
                    side = trade.side,
                ).netPNL
            }
        }
        .map { listOfPairs ->
            listOfPairs
                .asReversed()
                .groupingBy {
                    LocalDate(year = it.first.year, monthNumber = it.first.monthNumber, 1)
                }
                .fold(
                    initialValueSelector = { _, _ -> BigDecimal.ZERO },
                    operation = { _, accumulator, element -> accumulator + element.second },
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

    @Composable
    override fun render() {

        val themedOptions = themedChartOptions()
        val coroutineScope = rememberCoroutineScope()
        val arrangement = remember { ChartArrangement.single() }
        val chartPageState = remember { ChartPageState(coroutineScope, arrangement) }
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

    class Factory(private val appModule: AppModule) : Study.Factory<PNLByMonthChartStudy> {

        override val name: String = "PNL By Month (Chart)"

        override fun create() = PNLByMonthChartStudy(appModule)
    }
}
