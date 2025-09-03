package com.saurabhsandav.core.trading.data

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.toResultOr
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.ui.loginservice.fyers.FyersLoginService
import com.saurabhsandav.fyersapi.FyersApi
import com.saurabhsandav.fyersapi.model.CandleResolution
import com.saurabhsandav.fyersapi.model.DateFormat
import com.saurabhsandav.fyersapi.model.response.FyersError
import com.saurabhsandav.fyersapi.model.response.HistoricalCandlesResult
import com.saurabhsandav.fyersapi.model.response.isTokenExpired
import com.saurabhsandav.kbigdecimal.toKBigDecimal
import com.saurabhsandav.trading.candledata.CandleDownloader
import com.saurabhsandav.trading.core.Candle
import com.saurabhsandav.trading.core.SymbolId
import com.saurabhsandav.trading.core.Timeframe
import com.slack.eithernet.ApiResult
import com.slack.eithernet.ApiResult.Failure
import com.slack.eithernet.ApiResult.Success
import com.slack.eithernet.successOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

internal class FyersCandleDownloader(
    coroutineScope: CoroutineScope,
    private val appPrefs: FlowSettings,
    private val fyersApi: FyersApi,
) : CandleDownloader {

    private val isLoggedIn = FyersLoginService.getAuthTokensFromPrefs(appPrefs)
        .map { authTokens ->
            if (authTokens == null) return@map false

            // Check if access token expired
            fyersApi.getProfile(authTokens.accessToken).successOrNull() != null
        }
        .shareIn(
            scope = coroutineScope,
            started = SharingStarted.Lazily,
            replay = 1,
        )

    override fun isLoggedIn(): Flow<Boolean> = isLoggedIn

    override suspend fun download(
        symbolId: SymbolId,
        timeframe: Timeframe,
        from: Instant,
        to: Instant,
    ): Result<List<Candle>, CandleDownloader.Error> = coroutineBinding {

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
                accessToken = getAccessToken().bind(),
                symbol = symbolId.value,
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
            ).toResult().bind()

            // Add candles to result
            candles.addAll(result)

            // Go to next interval
            currentFrom = currentTo
            val newCurrentTo = currentFrom + downloadInterval
            currentTo = if (newCurrentTo > to) to else newCurrentTo
        }

        return@coroutineBinding candles
    }

    private suspend fun getAccessToken(): Result<String, CandleDownloader.Error> {
        return FyersLoginService.getAuthTokensFromPrefs(appPrefs).first()?.accessToken.toResultOr {
            CandleDownloader.Error.AuthError("Fyers not logged in")
        }
    }

    @Suppress("ktlint:standard:max-line-length")
    private fun ApiResult<HistoricalCandlesResult, FyersError>.toResult(): Result<List<Candle>, CandleDownloader.Error> {

        return when (this) {
            is Success -> {

                val candles = value.candles.map { candle ->
                    Candle(
                        openInstant = Instant.fromEpochSeconds(candle[0].toLong()),
                        open = candle[1].toKBigDecimal(),
                        high = candle[2].toKBigDecimal(),
                        low = candle[3].toKBigDecimal(),
                        close = candle[4].toKBigDecimal(),
                        volume = candle[5].toKBigDecimal(),
                    )
                }

                Ok(candles)
            }

            is Failure.ApiFailure -> when {
                error?.isTokenExpired == true -> Err(CandleDownloader.Error.AuthError(error?.message))
                else -> Err(CandleDownloader.Error.AuthError(error?.message ?: "Unknown Error"))
            }

            is Failure.HttpFailure -> Err(CandleDownloader.Error.UnknownError(error?.message ?: "Unknown Error"))
            is Failure.NetworkFailure -> Err(CandleDownloader.Error.UnknownError(error.message ?: "Unknown Error"))
            is Failure.UnknownFailure -> Err(CandleDownloader.Error.UnknownError(error.message ?: "Unknown Error"))
        }
    }
}
