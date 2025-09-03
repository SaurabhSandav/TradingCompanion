package com.saurabhsandav.trading.indicator

import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.kbigdecimal.toKBigDecimal
import com.saurabhsandav.trading.core.Indicator

class EMAIndicator(
    input: Indicator<KBigDecimal>,
    length: Int = 9,
) : AbstractEMAIndicator(
        input = input,
        multiplier = 2.toKBigDecimal().div(KBigDecimal.One + length.toKBigDecimal(), input.mathContext),
    )
