package trading.indicator.base

import trading.CandleSeries

abstract class RecursiveCachedIndicator<T : Any>(
    candleSeries: CandleSeries,
    description: String,
) : CachedIndicator<T>(candleSeries, description) {

    override fun get(index: Int): T {

        if (index <= candleSeries.lastIndex) {

            if (index > RECURSION_THRESHOLD) {

                for (toCacheIndex in 0 until index) {
                    super.get(toCacheIndex)
                }
            }
        }

        return super.get(index)
    }
}

private const val RECURSION_THRESHOLD = 100
