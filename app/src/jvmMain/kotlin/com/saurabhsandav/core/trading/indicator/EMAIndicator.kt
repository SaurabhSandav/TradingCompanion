package com.saurabhsandav.core.trading.indicator

import com.saurabhsandav.core.trading.Indicator
import java.math.BigDecimal

class EMAIndicator(
    input: Indicator<BigDecimal>,
    length: Int = 9,
) : AbstractEMAIndicator(
        input = input,
        multiplier = 2.toBigDecimal().divide(BigDecimal.ONE + length.toBigDecimal(), input.mathContext),
    )
