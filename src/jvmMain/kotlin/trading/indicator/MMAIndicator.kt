package trading.indicator

import trading.indicator.base.Indicator
import java.math.BigDecimal

class MMAIndicator(
    input: Indicator<BigDecimal>,
    length: Int,
) : AbstractEMAIndicator(
    input = input,
    multiplier = BigDecimal.ONE.divide(length.toBigDecimal(), input.mathContext),
)
