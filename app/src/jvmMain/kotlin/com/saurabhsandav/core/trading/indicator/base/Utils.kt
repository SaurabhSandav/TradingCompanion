package com.saurabhsandav.core.trading.indicator.base

import com.saurabhsandav.core.trading.core.Indicator

internal fun Indicator<*>.checkIndexValid(index: Int) {
    if (index < 0) error("Index cannot be negative: $index")
    if (index > candleSeries.lastIndex) error("Index out of candle series range: $index")
}
