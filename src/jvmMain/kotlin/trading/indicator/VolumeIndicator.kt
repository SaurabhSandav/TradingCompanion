package trading.indicator

import trading.CandleSeries
import trading.indicator.base.CachedIndicator
import java.math.BigDecimal
import kotlin.math.max

class VolumeIndicator(
    candleSeries: CandleSeries,
    private val length: Int = 1,
) : CachedIndicator<BigDecimal>(
    candleSeries = candleSeries,
    description = "Volume($length)",
) {

    override fun calculate(index: Int): BigDecimal {

        val startIndex = max(0, index - length + 1)
        var volumeSum = BigDecimal.ZERO

        for (i in startIndex..index) {
            volumeSum += candleSeries.list[i].volume
        }

        return volumeSum
    }
}
