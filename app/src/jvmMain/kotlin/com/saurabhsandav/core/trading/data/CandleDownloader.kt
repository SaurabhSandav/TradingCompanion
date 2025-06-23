package com.saurabhsandav.core.trading.data

import com.github.michaelbull.result.Result
import com.saurabhsandav.trading.core.Candle
import com.saurabhsandav.trading.core.Timeframe
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

interface CandleDownloader {

    fun isLoggedIn(): Flow<Boolean>

    suspend fun download(
        ticker: String,
        timeframe: Timeframe,
        from: Instant,
        to: Instant,
    ): Result<List<Candle>, Error>

    sealed class Error {

        class AuthError(
            val message: String?,
        ) : Error()

        class UnknownError(
            val message: String,
        ) : Error()
    }
}
