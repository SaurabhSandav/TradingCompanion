package com.saurabhsandav.kbigdecimal

public data class KMathContext(
    public val precision: Int,
    public val roundingMode: KRoundingMode? = null,
) {

    public companion object {

        public val Decimal32: KMathContext = KMathContext(7, KRoundingMode.HalfEven)
    }
}

public enum class KRoundingMode {
    Up,
    Down,
    Ceiling,
    Floor,
    HalfUp,
    HalfDown,
    HalfEven,
}
