package com.saurabhsandav.core.trading.data

import com.github.michaelbull.result.Result
import com.saurabhsandav.core.trading.Candle
import com.saurabhsandav.core.trading.Timeframe
import kotlinx.datetime.Instant

interface CandleDownloader {

    suspend fun download(
        symbol: String,
        timeframe: Timeframe,
        from: Instant,
        to: Instant,
    ): Result<List<Candle>, Error>

    sealed class Error {

        class AuthError(val message: String?) : Error()

        class UnknownError(val message: String) : Error()
    }
}
