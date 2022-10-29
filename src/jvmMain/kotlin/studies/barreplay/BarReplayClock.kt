package studies.barreplay

import kotlinx.datetime.Instant
import trading.Timeframe
import kotlin.time.Duration.Companion.seconds

internal class BarReplayClock(
    private val initialTime: Instant,
    private val initialIndex: Int,
    private val timeframe: Timeframe,
) {

    var currentTime = initialTime
        private set
    var currentIndex = initialIndex
        private set

    fun next() {
        currentTime += timeframe.seconds.seconds
        currentIndex++
    }

    fun reset() {
        currentTime = initialTime
        currentIndex = initialIndex
    }
}
