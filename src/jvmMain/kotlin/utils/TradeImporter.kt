package utils

import AppModule
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atTime
import java.io.File

internal class TradeImporter(
    private val appModule: AppModule,
) {

    fun importTrades() {

        val file = File("/home/saurabh/Downloads/Trade log - Historical Trades.csv")
        val rows: List<Map<String, String>> = csvReader {
            autoRenameDuplicateHeaders = true
        }.readAllWithHeader(file)

        val monthStr = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

        fun insertClosedTrade(row: Map<String, String>) {

            val dateValues = row.getValue("Date").split("-")
            val date = LocalDate(
                year = dateValues[2].toInt(),
                monthNumber = monthStr.indexOf(dateValues[1]) + 1,
                dayOfMonth = dateValues[0].toInt(),
            )

            val entryTimeValues = row.getValue("Entry Time").split(":")
            val entryTime = LocalTime(
                hour = entryTimeValues[0].toInt(),
                minute = entryTimeValues[1].toInt(),
                second = entryTimeValues[2].toInt(),
            )

            val exitTimeValues = row.getValue("Exit Time").split(":")
            val exitTime = LocalTime(
                hour = exitTimeValues[0].toInt(),
                minute = exitTimeValues[1].toInt(),
                second = exitTimeValues[2].toInt(),
            )

            appModule.appDB.closedTradeQueries.insert(
                id = null,
                broker = row.getValue("Broker"),
                ticker = row.getValue("Scrip"),
                instrument = row.getValue("Instrument").lowercase(),
                quantity = row.getValue("Qty"),
                lots = row["Lots"]?.toIntOrNull(),
                side = row.getValue("Side").lowercase(),
                entry = row.getValue("Entry"),
                stop = row.getValue("SL"),
                entryDate = date.atTime(entryTime).toString(),
                target = row.getValue("Target"),
                exit = row.getValue("Exit"),
                exitDate = date.atTime(exitTime).toString(),
            )
        }

        fun insertClosedTradeDetailed(row: Map<String, String>) {

            val id = appModule.appDB.closedTradeQueries.getAll().executeAsList().maxOf { it.id }

            appModule.appDB.closedTradeDetailQueries.insert(
                closedTradeId = id,
                maxFavorableExcursion = row.getValue("Maximum Favorable Excursion").ifBlank { null },
                maxAdverseExcursion = row.getValue("Maximum Adverse Excursion").ifBlank { null },
                persisted = row.getValue("Persisted"),
//                tags = row.getValue("Tags"),
                persistenceResult = row.getValue("Persistence Result"),
            )
        }

        rows.forEach { row ->

            appModule.appDB.transaction {
                insertClosedTrade(row)
                insertClosedTradeDetailed(row)
            }
        }
    }
}
