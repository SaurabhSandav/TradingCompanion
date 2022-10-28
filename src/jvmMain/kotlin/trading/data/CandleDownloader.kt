package trading.data

import com.github.michaelbull.result.Result
import kotlinx.datetime.Instant
import trading.Candle
import trading.Timeframe

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
