package utils

import AppModule
import com.russhwolf.settings.coroutines.FlowSettings
import fyers_api.FyersApi
import fyers_api.model.CandleResolution
import fyers_api.model.DateFormat
import kotlinx.coroutines.delay
import kotlinx.datetime.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import trading.Candle
import java.nio.file.Path
import kotlin.io.path.*

internal class CandleRepo(
    appModule: AppModule,
    private val json: Json = appModule.json,
    private val appPrefs: FlowSettings = appModule.appPrefs,
    private val fyersApi: FyersApi = appModule.fyersApiFactory(),
) {

    suspend fun getCandles(
        symbol: String,
        resolution: CandleResolution,
        from: Instant,
        to: Instant,
    ): List<Candle> {

        val accessToken = appPrefs.getStringOrNull(PrefKeys.FyersAccessToken) ?: error("Fyers not logged in!")

        val symbolFull = "NSE:$symbol-EQ"

        val baseDir = Path(AppPaths.getAppDataPath())
        val symbolDir = baseDir.resolve("Candles/$symbol/$resolution")

        if (!symbolDir.exists()) symbolDir.createDirectories()

        val currentTime = Clock.System.now()

        val fromDate = from.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val toDate = when {
            to > currentTime -> currentTime
            else -> to
        }.toLocalDateTime(TimeZone.currentSystemDefault()).date

        val fromMonth = LocalDate(year = fromDate.year, month = fromDate.month, dayOfMonth = 1)
        val toMonth = LocalDate(year = toDate.year, month = toDate.month, dayOfMonth = 1) + DatePeriod(months = 1)

        val months = buildList {
            var iMonth = fromMonth

            while (iMonth < toMonth) {

                add(iMonth)

                // Increment month
                iMonth += DatePeriod(months = 1)
            }
        }

        val unavailableCandleMonths = months.filter { !symbolDir.resolve(it.toString()).exists() }

        if (unavailableCandleMonths.isNotEmpty()) {

            cacheCandles(
                accessToken = accessToken,
                symbolDir = symbolDir,
                symbolFull = symbolFull,
                resolution = resolution,
                months = unavailableCandleMonths,
            )
        }

        val candles = months.map { month ->

            val monthFilePath = symbolDir.resolve(month.toString())
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

        return candles
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

            // Create file for symbol
            val monthFilePath = symbolDir.resolve(month.toString())
            if (!monthFilePath.exists()) monthFilePath.createFile()

            // End date = 1st of next Month
            val endDate = month + DatePeriod(months = 1) - DatePeriod(days = 1)

            // Download
            val history = fyersApi.getHistoricalCandles(
                accessToken = accessToken,
                symbol = symbolFull,
                resolution = resolution,
                dateFormat = DateFormat.YYYY_MM_DD,
                rangeFrom = month.toString(),
                rangeTo = endDate.toString(),
            )

            // Write to file
            monthFilePath.writeText(json.encodeToString(history))

            // API Rate limit
            delay(400)
        }
    }
}
