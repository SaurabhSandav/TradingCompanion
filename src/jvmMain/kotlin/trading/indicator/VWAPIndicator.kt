package trading.indicator

import trading.CandleSeries
import trading.indicator.base.CachedIndicator
import java.math.BigDecimal

class VWAPIndicator(
    candleSeries: CandleSeries,
    isSessionStart: (CandleSeries, index: Int) -> Boolean,
) : CachedIndicator<BigDecimal>(
    candleSeries = candleSeries,
    description = null,
) {

    private val typicalPrice = TypicalPriceIndicator(candleSeries)
    private val volume = VolumeIndicator(candleSeries)
    private val tpv = ProductIndicator(typicalPrice, volume)
    private val cumulativeTPV = SessionCumulativeIndicator(tpv, isSessionStart)
    private val cumulativeVolume = SessionCumulativeIndicator(volume, isSessionStart)

    override fun calculate(index: Int): BigDecimal {
        return when {
            cumulativeVolume[index] == BigDecimal.ZERO -> BigDecimal.ZERO
            else -> cumulativeTPV[index].divide(cumulativeVolume[index], mathContext)
        }
    }
}
