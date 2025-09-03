package com.saurabhsandav.kbigdecimal

public data class KMathContext(
    public val precision: Int,
    public val roundingMode: KRoundingMode? = null,
)

public enum class KRoundingMode {
    Up,
    Down,
    Ceiling,
    Floor,
    HalfUp,
    HalfDown,
    HalfEven,
}
