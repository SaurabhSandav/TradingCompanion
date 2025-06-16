package com.saurabhsandav.core.trading

import com.saurabhsandav.kbigdecimal.KBigDecimal

fun isValidPrice(
    price: KBigDecimal,
    tickSize: KBigDecimal,
): Boolean = price.remainder(tickSize).compareTo(KBigDecimal.Zero) == 0

fun isValidQuantity(
    quantity: KBigDecimal,
    lotSize: KBigDecimal,
): Boolean = quantity.remainder(lotSize).compareTo(KBigDecimal.Zero) == 0
