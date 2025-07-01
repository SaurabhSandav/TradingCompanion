package com.saurabhsandav.trading.indicator

import com.saurabhsandav.trading.core.Indicator
import java.math.BigDecimal

internal fun Indicator<*>.checkIndexValid(index: Int) {
    if (index < 0) error("Index cannot be negative: $index")
    if (index > candleSeries.lastIndex) error("Index out of candle series range: $index")
}

internal fun BigDecimal.isZero(): Boolean = compareTo(BigDecimal.ZERO) == 0
