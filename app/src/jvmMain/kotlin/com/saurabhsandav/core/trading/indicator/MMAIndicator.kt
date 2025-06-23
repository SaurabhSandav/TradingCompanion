package com.saurabhsandav.core.trading.indicator

import com.saurabhsandav.trading.core.Indicator
import java.math.BigDecimal

class MMAIndicator(
    input: Indicator<BigDecimal>,
    length: Int,
) : AbstractEMAIndicator(
        input = input,
        multiplier = BigDecimal.ONE.divide(length.toBigDecimal(), input.mathContext),
    )
