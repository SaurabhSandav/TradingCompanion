package studies

import AppModule
import chart.Chart
import chart.baseline.BaselineData
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toLocalDateTime
import model.Side
import utils.brokerage
import java.math.BigDecimal
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal class PNLByDayChartStudy(
    appModule: AppModule,
) : ChartStudy() {

    override val name: String = "PNL By Day (Chart)"

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
                    BaselineData(
                        time = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(localDate.toJavaLocalDate()),
                        value = bigDecimal,
                    )
                }
        }

    override fun CoroutineScope.configureChart(chart: Chart) {

        val baselineSeries = chart.addBaselineSeries()

        launch {
            data.collect {
                baselineSeries.setData(it)
                chart.timeScale.fitContent()
            }
        }
    }
}
