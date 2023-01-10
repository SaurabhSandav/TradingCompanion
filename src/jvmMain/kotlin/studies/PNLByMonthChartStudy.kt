package studies

import AppModule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import chart.baselineSeries
import chart.createChart
import chart.data.SingleValueData
import chart.data.Time
import chart.options.CrosshairMode
import chart.options.CrosshairOptions
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime
import model.Side
import ui.common.chart.ChartPage
import ui.common.chart.state.ChartPageState
import ui.common.chart.themedChartOptions
import utils.brokerage
import java.math.BigDecimal

internal class PNLByMonthChartStudy(
    appModule: AppModule,
) : Study {

    private val data = appModule.appDB
        .closedTradeQueries
        .getAllClosedTradesDetailed { _, broker, _, instrument, quantity, _, side, entry, _, entryDate, _, exit, _, _, _, _, _, _ ->

            val entryBD = entry.toBigDecimal()
            val exitBD = exit.toBigDecimal()
            val quantityBD = quantity.toBigDecimal()
            val sideEnum = Side.fromString(side)

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
                .groupingBy {
                    LocalDate(year = it.first.year, monthNumber = it.first.monthNumber, 1)
                }
                .fold(
                    initialValueSelector = { _, _ -> BigDecimal.ZERO },
                    operation = { _, accumulator, element -> accumulator + element.second },
                )
                .map { (localDate, bigDecimal) ->
                    SingleValueData(
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

        val chart = remember {
            createChart(options = themedOptions.copy(crosshair = CrosshairOptions(mode = CrosshairMode.Normal)))
        }

        val chartPageState = remember { ChartPageState(coroutineScope, chart) }

        ChartPage(chartPageState)

        LaunchedEffect(chart) {

            val baselineSeries by chart.baselineSeries()

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
