package com.saurabhsandav.core.studies

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
import com.saurabhsandav.core.trades.model.TradeSide
import com.saurabhsandav.core.ui.common.chart.ChartPage
import com.saurabhsandav.core.ui.common.chart.arrangement.ChartArrangement
import com.saurabhsandav.core.ui.common.chart.arrangement.single
import com.saurabhsandav.core.ui.common.chart.crosshairMove
import com.saurabhsandav.core.ui.common.chart.state.ChartPageState
import com.saurabhsandav.core.ui.common.chart.themedChartOptions
import com.saurabhsandav.core.utils.brokerage
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal

internal class PNLByDayChartStudy(
    appModule: AppModule,
) : Study {

    private val data = appModule.appDB
        .closedTradeQueries
        .getAll { _, broker, _, instrument, quantity, _, side, entry, _, entryDate, _, exit, _ ->

            val entryBD = entry.toBigDecimal()
            val exitBD = exit.toBigDecimal()
            val quantityBD = quantity.toBigDecimal()
            val sideEnum = TradeSide.fromString(side)

            entryDate.toLocalDateTime().date to brokerage(
                broker = broker,
                instrument = instrument,
                entry = entryBD,
                exit = exitBD,
                quantity = quantityBD,
                side = sideEnum,
            ).netPNL
        }
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map { listOfPairs ->
            listOfPairs
                .asReversed()
                .groupingBy { it.first }
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

    class Factory(private val appModule: AppModule) : Study.Factory<PNLByDayChartStudy> {

        override val name: String = "PNL By Day (Chart)"

        override fun create() = PNLByDayChartStudy(appModule)
    }
}
