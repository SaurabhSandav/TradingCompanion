package studies

import AppModule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import chart.baselineSeries
import chart.createChart
import chart.data.SingleValueData
import chart.data.Time
import chart.options.ChartOptions
import chart.options.CrosshairMode
import chart.options.CrosshairOptions
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.datetime.toLocalDateTime
import model.Side
import ui.common.ResizableChart
import utils.brokerage
import java.math.BigDecimal

internal class PNLByDayChartStudy(
    appModule: AppModule,
) : Study {

    private val data = appModule.appDB
        .closedTradeQueries
        .getAllClosedTradesDetailed { _, broker, _, instrument, quantity, _, side, entry, _, entryDate, _, exit, _, _, _, _, _ ->

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
            )
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

        val chart = remember {
            createChart(ChartOptions(crosshair = CrosshairOptions(mode = CrosshairMode.Normal)))
        }

        ResizableChart(chart)

        LaunchedEffect(chart) {

            val baselineSeries by chart.baselineSeries()

            data.collect {
                baselineSeries.setData(it)
                chart.timeScale.fitContent()
            }
        }
    }

    class Factory(private val appModule: AppModule) : Study.Factory<PNLByDayChartStudy> {

        override val name: String = "PNL By Day (Chart)"

        override fun create() = PNLByDayChartStudy(appModule)
    }
}
