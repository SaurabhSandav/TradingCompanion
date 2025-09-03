package com.saurabhsandav.kbigdecimal

public actual fun String.toKBigDecimalOrNull(): KBigDecimal? = toBigDecimalOrNull()?.let(::KBigDecimal)

public actual fun KBigDecimal.toDouble(): Double = value.toDouble()

public actual fun KBigDecimal.isEqualTo(other: KBigDecimal): Boolean = value.compareTo(other.value) == 0

public actual fun KBigDecimal.isZero(): Boolean = isEqualTo(KBigDecimal.Zero)
