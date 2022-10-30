package studies.barreplay

import kotlinx.datetime.Instant
import trading.CandleSeries

internal class BarReplayClock(
    private val initialIndex: Int,
    private val initialTime: Instant,
) {

    var currentTime = initialTime
        private set
    var currentIndex = initialIndex
        private set

    fun next(baseCandleSeries: CandleSeries) {
        currentIndex++
        currentTime = baseCandleSeries.list[currentIndex].openInstant
    }

    fun reset() {
        currentTime = initialTime
        currentIndex = initialIndex
    }
}
