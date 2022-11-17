package trading.data

import AppModule
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.russhwolf.settings.coroutines.FlowSettings
import fyers_api.FyersApi
import fyers_api.model.CandleResolution
import fyers_api.model.DateFormat
import io.ktor.http.*
import kotlinx.datetime.Instant
import trading.Candle
import trading.Timeframe
import utils.PrefKeys

internal class FyersCandleDownloader(
    appModule: AppModule,
    private val appPrefs: FlowSettings = appModule.appPrefs,
    private val fyersApi: FyersApi = appModule.fyersApiFactory(),
) : CandleDownloader {

    private var accessToken: String? = null

    override suspend fun download(
        symbol: String,
        timeframe: Timeframe,
        from: Instant,
        to: Instant,
    ): Result<List<Candle>, CandleDownloader.Error> {

        if (accessToken == null) {
            accessToken = appPrefs.getStringOrNull(PrefKeys.FyersAccessToken)
                ?: return Err(CandleDownloader.Error.AuthError("Fyers not logged in"))
        }

        // Fyers symbol notation
        val symbolFull = when (symbol) {
            "NIFTY50" -> "NSE:$symbol-INDEX"
            else -> "NSE:$symbol-EQ"
        }

        val response = fyersApi.getHistoricalCandles(
            accessToken = accessToken!!,
            symbol = symbolFull,
            resolution = when (timeframe) {
                Timeframe.M1 -> CandleResolution.M1
                Timeframe.M5 -> CandleResolution.M5
                Timeframe.D1 -> CandleResolution.D1
            },
            dateFormat = DateFormat.EPOCH,
            rangeFrom = from.epochSeconds.toString(),
            rangeTo = to.epochSeconds.toString(),
        )

        return when (response.result) {
            null -> when (response.statusCode) {
                HttpStatusCode.Unauthorized -> Err(CandleDownloader.Error.AuthError(response.message))
                else -> Err(CandleDownloader.Error.UnknownError(response.message ?: "Unknown Error"))
            }

            else -> {

                val candles = response.result.candles.map { candle ->
                    Candle(
                        openInstant = Instant.fromEpochSeconds(candle[0].toLong()),
                        open = candle[1].toBigDecimal(),
                        high = candle[2].toBigDecimal(),
                        low = candle[3].toBigDecimal(),
                        close = candle[4].toBigDecimal(),
                        volume = candle[5].toBigDecimal(),
                    )
                }

                Ok(candles)
            }
        }
    }
}
