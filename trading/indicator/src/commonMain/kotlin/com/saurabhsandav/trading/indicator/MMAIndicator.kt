package com.saurabhsandav.trading.indicator

import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.kbigdecimal.toKBigDecimal
import com.saurabhsandav.trading.core.Indicator

class MMAIndicator(
    input: Indicator<KBigDecimal>,
    length: Int,
) : AbstractEMAIndicator(
        input = input,
        multiplier = KBigDecimal.One.div(length.toKBigDecimal(), input.mathContext),
    )
