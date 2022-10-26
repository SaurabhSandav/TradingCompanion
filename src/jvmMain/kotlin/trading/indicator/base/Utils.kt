package trading.indicator.base

internal fun Indicator<*>.checkIndexValid(index: Int) {
    if (index < 0) error("Index cannot be negative: $index")
    if (index > candleSeries.list.lastIndex) error("Index out of candle series range: $index")
}
