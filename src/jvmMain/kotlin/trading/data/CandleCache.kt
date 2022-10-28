package trading.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import trading.Candle
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

interface CandleCache {

    suspend fun getAvailableCandleRange(
        symbolDir: Path,
    ): ClosedRange<Instant>?

    suspend fun fetch(
        symbolDir: Path,
        from: Instant,
        to: Instant,
    ): List<Candle>

    suspend fun writeCandles(
        symbolDir: Path,
        candles: List<Candle>,
    )
}

class CandleCacheImpl : CandleCache {

    private val json = Json { prettyPrint = true }

    override suspend fun getAvailableCandleRange(
        symbolDir: Path,
    ): ClosedRange<Instant>? = withContext(Dispatchers.IO) {

        // Get files for symbol sorted by date
        val allFiles = Files.list(symbolDir).toList().sortedBy { LocalDate.parse(it.fileName.toString().dropLast(4)) }

        if (allFiles.isEmpty()) return@withContext null

        fun JsonElement.toCandle() = Candle(
            openInstant = Instant.fromEpochSeconds(jsonArray[0].jsonPrimitive.long),
            open = jsonArray[1].jsonPrimitive.content.toBigDecimal(),
            high = jsonArray[2].jsonPrimitive.content.toBigDecimal(),
            low = jsonArray[3].jsonPrimitive.content.toBigDecimal(),
            close = jsonArray[4].jsonPrimitive.content.toBigDecimal(),
            volume = jsonArray[5].jsonPrimitive.content.toBigDecimal(),
        )

        val startInstant = run {
            val dataJson = json.parseToJsonElement(allFiles.first().readText())

            dataJson.jsonArray.first().toCandle().openInstant
        }
        val endInstant = run {
            val dataJson = json.parseToJsonElement(allFiles.last().readText())

            dataJson.jsonArray.last().toCandle().openInstant
        }

        return@withContext startInstant..endInstant
    }

    override suspend fun fetch(
        symbolDir: Path,
        from: Instant,
        to: Instant,
    ): List<Candle> = withContext(Dispatchers.IO) {

        // Get files for symbol sorted by date
        val allFiles = Files.list(symbolDir).toList().sortedBy { LocalDate.parse(it.fileName.toString().dropLast(4)) }

        if (allFiles.isEmpty()) return@withContext emptyList()

        val fetchFiles = allFiles.filter {
            val fileMonth = LocalDate.parse(it.fileName.toString().dropLast(4))
            fileMonth.atStartOfDayIn(TimeZone.currentSystemDefault()) in from..to
        }

        fun JsonElement.toCandle() = Candle(
            openInstant = Instant.fromEpochSeconds(jsonArray[0].jsonPrimitive.long),
            open = jsonArray[1].jsonPrimitive.content.toBigDecimal(),
            high = jsonArray[2].jsonPrimitive.content.toBigDecimal(),
            low = jsonArray[3].jsonPrimitive.content.toBigDecimal(),
            close = jsonArray[4].jsonPrimitive.content.toBigDecimal(),
            volume = jsonArray[5].jsonPrimitive.content.toBigDecimal(),
        )

        return@withContext fetchFiles.map { path ->

            val dataJson = json.parseToJsonElement(path.readText())

            dataJson.jsonArray.map { it.toCandle() }
        }.flatten().filter { it.openInstant in from..to }
    }

    override suspend fun writeCandles(
        symbolDir: Path,
        candles: List<Candle>,
    ) = withContext(Dispatchers.IO) {

        candles.groupBy { it.openInstant.toLocalDateTime(TimeZone.currentSystemDefault()).month }
            .mapKeys { (_, candles) ->
                val firstCandleDate = candles.first().openInstant.toLocalDateTime(TimeZone.currentSystemDefault())
                LocalDate(year = firstCandleDate.year, month = firstCandleDate.month, dayOfMonth = 1)
            }
            .forEach { (firstDayOfMonth, candles) ->

                // Create file for symbol
                val monthFilePath = symbolDir.resolve("$firstDayOfMonth.txt")
                val prevCachedCandles = when {
                    !monthFilePath.exists() -> {
                        monthFilePath.createFile()
                        emptyList()
                    }

                    else -> {

                        val dataTxt = monthFilePath.readText()
                        when {
                            dataTxt.isEmpty() -> emptyList()
                            else -> {

                                val dataJson = json.parseToJsonElement(dataTxt)

                                fun JsonElement.toCandle() = Candle(
                                    openInstant = Instant.fromEpochSeconds(jsonArray[0].jsonPrimitive.long),
                                    open = jsonArray[1].jsonPrimitive.content.toBigDecimal(),
                                    high = jsonArray[2].jsonPrimitive.content.toBigDecimal(),
                                    low = jsonArray[3].jsonPrimitive.content.toBigDecimal(),
                                    close = jsonArray[4].jsonPrimitive.content.toBigDecimal(),
                                    volume = jsonArray[5].jsonPrimitive.content.toBigDecimal(),
                                )

                                dataJson.jsonArray.map { it.toCandle() }
                            }
                        }
                    }
                }

                val allCandles = (prevCachedCandles + candles).sortedBy { it.openInstant }.distinctBy { it.openInstant }

                val candleJsonArray = buildJsonArray {
                    allCandles.forEach { candle ->
                        add(
                            JsonArray(
                                listOf(
                                    JsonPrimitive(candle.openInstant.epochSeconds),
                                    JsonPrimitive(candle.open.toPlainString()),
                                    JsonPrimitive(candle.high.toPlainString()),
                                    JsonPrimitive(candle.low.toPlainString()),
                                    JsonPrimitive(candle.close.toPlainString()),
                                    JsonPrimitive(candle.volume.toPlainString()),
                                )
                            )
                        )
                    }
                }

                // Write to file
                monthFilePath.writeText(json.encodeToString(candleJsonArray))
            }
    }
}
