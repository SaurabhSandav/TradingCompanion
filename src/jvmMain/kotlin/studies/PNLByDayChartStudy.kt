package studies

import AppDensityFraction
import AppModule
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.datetime.toLocalDateTime
import model.Side
import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.JFreeChart
import org.jfree.chart.axis.NumberAxis
import org.jfree.chart.plot.PlotOrientation
import org.jfree.chart.plot.ValueMarker
import org.jfree.chart.plot.XYPlot
import org.jfree.data.time.Day
import org.jfree.data.time.TimeSeries
import org.jfree.data.time.TimeSeriesCollection
import org.jfree.data.xy.IntervalXYDataset
import utils.brokerage
import java.awt.Color
import java.math.BigDecimal
import java.text.NumberFormat


internal class PNLByDayChartStudy(appModule: AppModule) : Study {

    override val name: String = "PNL By Day (Chart)"

    private val series = TimeSeries("PNL")

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
            listOfPairs.groupingBy { it.first }
                .fold(
                    initialValueSelector = { _, _ -> BigDecimal.ZERO },
                    operation = { _, accumulator, element -> accumulator + element.second },
                )
        }

    @Composable
    override fun render() {

        val timeSeriesCollection = remember { TimeSeriesCollection(series) }

        LaunchedEffect(series, data) {

            data.collect {

                it.forEach { (date, value) ->
                    val day = Day(date.dayOfMonth, date.monthNumber, date.year)
                    series.addOrUpdate(day, value)
                }
            }
        }

        Chart(timeSeriesCollection)
    }
}

@Composable
private fun Chart(
    xyDataset: IntervalXYDataset,
) {

    SwingPanel(
        modifier = Modifier.fillMaxSize(AppDensityFraction),
        factory = {

            val chart: JFreeChart = ChartFactory.createXYBarChart(
                null,
                "Date",
                true,
                "PNL",
                xyDataset,
                PlotOrientation.VERTICAL,
                true, true, false
            )

            val plot: XYPlot = chart.xyPlot

            val marker = ValueMarker(0.0)
            marker.paint = Color.black
            plot.addRangeMarker(marker)

            val currency = NumberFormat.getCurrencyInstance()
            currency.maximumFractionDigits = 0

            val rangeAxis = plot.rangeAxis as NumberAxis
            rangeAxis.numberFormatOverride = currency

            ChartPanel(chart)
        }
    )
}
