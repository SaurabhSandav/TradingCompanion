package trading

import kotlinx.coroutines.flow.Flow
import trading.indicator.base.IndicatorCache
import java.math.MathContext

interface CandleSeries : List<Candle> {

    val timeframe: Timeframe

    val live: Flow<Candle>

    val indicatorMathContext: MathContext

    fun <T> getIndicatorCache(key: String?): IndicatorCache<T>
}
