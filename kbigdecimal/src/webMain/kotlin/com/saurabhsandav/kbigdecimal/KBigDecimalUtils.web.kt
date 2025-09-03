package com.saurabhsandav.kbigdecimal

import kotlin.js.toDouble

public actual fun String.toKBigDecimalOrNull(): KBigDecimal? {

    val value = BigNumber(this)

    if (value.isNaN()) return null

    return KBigDecimal(value)
}

public actual fun KBigDecimal.toDouble(): Double = value.toNumber().toDouble()

public actual fun KBigDecimal.isEqualTo(other: KBigDecimal): Boolean = value.isEqualTo(other.value)

public actual fun KBigDecimal.isZero(): Boolean = value.isZero()
