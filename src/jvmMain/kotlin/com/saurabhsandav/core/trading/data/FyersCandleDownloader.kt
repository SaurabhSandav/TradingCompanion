package com.saurabhsandav.core.trading.data

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.fyers_api.FyersApi
import com.saurabhsandav.core.fyers_api.model.CandleResolution
import com.saurabhsandav.core.fyers_api.model.DateFormat
import com.saurabhsandav.core.fyers_api.model.response.FyersResponse
import com.saurabhsandav.core.fyers_api.model.response.HistoricalCandlesResult
import com.saurabhsandav.core.trading.Candle
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.utils.PrefKeys
import io.ktor.http.*
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days

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

        val candles = mutableListOf<Candle>()
        val downloadInterval = when (timeframe) {
            Timeframe.D1 -> 365.days // Fyers accepts a 1-year range for 1D timeframe
            else -> 99.days // Fyers accepts a 100-day range for less than 1D timeframes
        }
        val requestedInterval = to - from

        // First interval
        var currentFrom = from
        var currentTo = if (requestedInterval > downloadInterval) from + downloadInterval else to

        // While complete interval is not exhausted
        while (currentTo <= to && currentFrom != currentTo) {

            // Download and convert to result
            val result = fyersApi.getHistoricalCandles(
                accessToken = accessToken!!,
                symbol = symbolFull,
                resolution = when (timeframe) {
                    Timeframe.M1 -> CandleResolution.M1
                    Timeframe.M3 -> CandleResolution.M3
                    Timeframe.M5 -> CandleResolution.M5
                    Timeframe.M15 -> CandleResolution.M15
                    Timeframe.M30 -> CandleResolution.M30
                    Timeframe.H1 -> CandleResolution.M60
                    Timeframe.H4 -> CandleResolution.M240
                    Timeframe.D1 -> CandleResolution.D1
                },
                dateFormat = DateFormat.EPOCH,
                rangeFrom = currentFrom.epochSeconds.toString(),
                rangeTo = currentTo.epochSeconds.toString(),
            ).toResult()

            // Unwrap result
            when (result) {
                is Err -> return result
                is Ok -> candles.addAll(result.value)
            }

            // Go to next interval
            currentFrom = currentTo
            val newCurrentTo = currentFrom + downloadInterval
            currentTo = if (newCurrentTo > to) to else newCurrentTo
        }

        return Ok(candles)
    }

    private fun FyersResponse<HistoricalCandlesResult>.toResult(): Result<List<Candle>, CandleDownloader.Error> {
        return when (result) {
            null -> when (statusCode) {
                HttpStatusCode.Unauthorized -> Err(CandleDownloader.Error.AuthError(message))
                else -> Err(CandleDownloader.Error.UnknownError(message ?: "Unknown Error"))
            }

            else -> {

                val candles = result.candles.map { candle ->
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
