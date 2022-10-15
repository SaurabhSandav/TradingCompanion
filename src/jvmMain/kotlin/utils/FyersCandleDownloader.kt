package utils

import fyers_api.FyersApi
import fyers_api.model.CandleResolution
import fyers_api.model.DateFormat
import kotlinx.coroutines.delay
import kotlinx.datetime.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.io.path.*

class FyersCandleDownloader(
    private val fyersApi: FyersApi,
) {

    suspend fun download(
        accessToken: String,
        symbol: String,
        resolution: CandleResolution,
    ) {

        val symbolFull = "NSE:$symbol-EQ"

        val json = Json { prettyPrint = true }

        val basePath = Path("/home/saurabh/Downloads/Candles/")
        if (!basePath.exists()) error("Base Path for candles does not exist")
        val symbolPath = basePath.resolve("$symbol/$resolution")
        if (!symbolPath.exists()) symbolPath.createDirectories()

        // 1 Year starting from same Month of last year
        val startDate = LocalDate(year = 2021, month = Month.OCTOBER, dayOfMonth = 1)
        val endDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        var iMonth = startDate

        while (iMonth < endDate) {

            val monthFilePath = symbolPath.resolve(iMonth.toString())
            if (!monthFilePath.exists()) monthFilePath.createFile()

            val endDay = iMonth + DatePeriod(months = 1) - DatePeriod(days = 1)

            val history = fyersApi.getHistoricalCandles(
                accessToken = accessToken,
                symbol = symbolFull,
                resolution = resolution,
                dateFormat = DateFormat.YYYY_MM_DD,
                rangeFrom = iMonth.toString(),
                rangeTo = endDay.toString(),
            )

            monthFilePath.writeText(json.encodeToString(history))

            iMonth += DatePeriod(months = 1)
            delay(400)
        }
    }
}
