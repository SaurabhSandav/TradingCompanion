package studies

import AppModule
import chart.IChartApi
import chart.series.SingleValueData
import chart.series.Time
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime
import model.Side
import utils.brokerage
import java.math.BigDecimal

internal class PNLByMonthChartStudy(
    appModule: AppModule,
) : ChartStudy() {

    override val name: String = "PNL By Month (Chart)"

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
                .groupingBy {
                    LocalDate(year = it.first.year, monthNumber = it.first.monthNumber, 1)
                }
                .fold(
                    initialValueSelector = { _, _ -> BigDecimal.ZERO },
                    operation = { _, accumulator, element -> accumulator + element.second },
                )
                .map { (localDate, bigDecimal) ->
                    SingleValueData(
//                        time = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(localDate.toJavaLocalDate()),
                        time = Time.BusinessDay(
                            year = localDate.year,
                            month = localDate.monthNumber,
                            day = localDate.dayOfMonth,
                        ),
                        value = bigDecimal,
                    )
                }
        }

    override fun CoroutineScope.configureChart(chart: IChartApi) {

        val baselineSeries = chart.addBaselineSeries()

        launch {
            data.collect {
                baselineSeries.setData(it)
                chart.timeScale.fitContent()
            }
        }
    }
}
