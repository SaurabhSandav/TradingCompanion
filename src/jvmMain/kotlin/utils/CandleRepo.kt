package utils

import AppModule
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.AppDB
import fyers_api.FyersApi
import fyers_api.model.response.FyersResponse
import fyers_api.model.CandleResolution
import fyers_api.model.DateFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.datetime.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import trading.Candle
import trading.CandleSeries
import java.nio.file.Path
import kotlin.io.path.*

internal class CandleRepo(
    appModule: AppModule,
    private val json: Json = appModule.json,
    private val appPrefs: FlowSettings = appModule.appPrefs,
    private val appDB: AppDB = appModule.appDB,
    private val fyersApi: FyersApi = appModule.fyersApiFactory(),
) {

    suspend fun getCandles(
        symbol: String,
        resolution: CandleResolution,
        from: Instant,
        to: Instant,
    ): CandleSeries {

        val accessToken = appPrefs.getStringOrNull(PrefKeys.FyersAccessToken) ?: error("Fyers not logged in!")

        // Fyers symbol notation
        val symbolFull = "NSE:$symbol-EQ"

        // Build directory path for symbol and timeframe
        val baseDir = Path(AppPaths.getAppDataPath())
        val symbolDir = baseDir.resolve("Candles/$symbol/$resolution")

        // Create directories if not exists
        if (!symbolDir.exists()) symbolDir.createDirectories()

        val currentTime = Clock.System.now()

        val fromDate = from.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val toDate = when {
            to > currentTime -> currentTime
            else -> to
        }.toLocalDateTime(TimeZone.currentSystemDefault()).date

        // Collect all candles for every month at once
        val months = buildList {

            // From 1st of the month of 'from' date
            var iMonth = LocalDate(year = fromDate.year, month = fromDate.month, dayOfMonth = 1)

            // To 1st of the month after the 'to' date
            val toMonth = LocalDate(year = toDate.year, month = toDate.month, dayOfMonth = 1) + DatePeriod(months = 1)

            while (iMonth < toMonth) {

                add(iMonth)

                // Increment month
                iMonth += DatePeriod(months = 1)
            }
        }

        // Get last sync month
        val lastSyncMonth = withContext(Dispatchers.IO) {
            val dateStr =
                appDB.candleLastSyncQueries.getLastUpdateDate(symbol).executeAsOneOrNull() ?: return@withContext null
            val date = LocalDate.parse(dateStr)
            LocalDate(year = date.year, month = date.month, dayOfMonth = 1)
        }

        // Get non-cached months for symbol
        // 1. Filter months when file does not exist
        // 2. Filter months greater than or equal to last sync month
        val unavailableCandleMonths = months.filter {
            !symbolDir.resolve(it.toString()).exists() || if (lastSyncMonth != null) it >= lastSyncMonth else false
        }

        // Cache candles if necessary
        if (unavailableCandleMonths.isNotEmpty()) {

            cacheCandles(
                accessToken = accessToken,
                symbolDir = symbolDir,
                symbolFull = symbolFull,
                resolution = resolution,
                months = unavailableCandleMonths,
            )
        }

        // Update last sync date
        withContext(Dispatchers.IO) {
            appDB.candleLastSyncQueries.update(ticker = symbol, lastUpdateDate = toDate.toString())
        }

        val series = CandleSeries()

        // Parse json to Candles
        // Filter out extra candles from before and after the provided range
        // Add to CandleSeries
        months.map { month ->

            val monthFilePath = symbolDir.resolve(month.toString())
            val dataTxt = monthFilePath.readText()
            if (dataTxt.isEmpty()) return@map emptyList()

            val dataJson = json.parseToJsonElement(monthFilePath.readText())

            dataJson.jsonArray.map {

                Candle(
                    openInstant = Instant.fromEpochSeconds(it.jsonArray[0].jsonPrimitive.long),
                    open = it.jsonArray[1].jsonPrimitive.content.toBigDecimal(),
                    high = it.jsonArray[2].jsonPrimitive.content.toBigDecimal(),
                    low = it.jsonArray[3].jsonPrimitive.content.toBigDecimal(),
                    close = it.jsonArray[4].jsonPrimitive.content.toBigDecimal(),
                    volume = it.jsonArray[5].jsonPrimitive.content.toBigDecimal(),
                )
            }
        }.flatten()
            .filter { it.openInstant in from..to }
            .forEach(series::addCandle)

        return series
    }

    private suspend fun cacheCandles(
        accessToken: String,
        symbolDir: Path,
        symbolFull: String,
        resolution: CandleResolution,
        months: List<LocalDate>,
    ) {

        val json = Json { prettyPrint = true }

        months.forEach { month ->

            // End date = 1st of next Month
            val endDate = month + DatePeriod(months = 1) - DatePeriod(days = 1)

            // Download candles for the month
            val response = fyersApi.getHistoricalCandles(
                accessToken = accessToken,
                symbol = symbolFull,
                resolution = resolution,
                dateFormat = DateFormat.YYYY_MM_DD,
                rangeFrom = month.toString(),
                rangeTo = endDate.toString(),
            )

            val candles = when (response) {
                is FyersResponse.Failure -> error(response.message)
                is FyersResponse.Success -> response.result.candles
            }

            // Create file for symbol
            val monthFilePath = symbolDir.resolve(month.toString())
            if (!monthFilePath.exists()) monthFilePath.createFile()

            // Write to file
            monthFilePath.writeText(json.encodeToString(candles))

            // API Rate limit
            delay(400)
        }
    }
}
