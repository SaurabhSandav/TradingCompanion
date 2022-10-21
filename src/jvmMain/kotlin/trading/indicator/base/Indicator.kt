package trading.indicator.base

import trading.CandleSeries
import java.math.MathContext

interface Indicator<T : Any> {

    val candleSeries: CandleSeries

    val mathContext: MathContext
        get() = candleSeries.indicatorMathContext

    val description: String?

    operator fun get(index: Int): T
}
