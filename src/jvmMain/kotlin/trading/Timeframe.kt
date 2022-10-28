package trading

import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

enum class Timeframe(val seconds: Long) {
    M1(1.minutes.inWholeSeconds),
    M5(5.minutes.inWholeSeconds),
    D1(1.days.inWholeSeconds);

    companion object {

        fun fromSeconds(seconds: Long): Timeframe? = when (seconds) {
            M1.seconds -> M1
            M5.seconds -> M5
            D1.seconds -> D1
            else -> null
        }
    }
}
