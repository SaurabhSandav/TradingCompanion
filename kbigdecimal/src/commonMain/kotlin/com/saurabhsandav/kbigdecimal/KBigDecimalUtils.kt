package com.saurabhsandav.kbigdecimal

import kotlin.experimental.ExperimentalTypeInference
import kotlin.jvm.JvmName

public fun String.toKBigDecimal(): KBigDecimal = KBigDecimal(this)

public expect fun String.toKBigDecimalOrNull(): KBigDecimal?

public fun Int.toKBigDecimal(): KBigDecimal = KBigDecimal(toString())

public fun Long.toKBigDecimal(): KBigDecimal = KBigDecimal(toString())

public fun Double.toKBigDecimal(): KBigDecimal = KBigDecimal(toString())

public expect fun KBigDecimal.toDouble(): Double

public expect fun KBigDecimal.isEqualTo(other: KBigDecimal): Boolean

public expect fun KBigDecimal.isZero(): Boolean

@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
@JvmName("sumOfKBigDecimal")
public inline fun <T> Iterable<T>.sumOf(selector: (T) -> KBigDecimal): KBigDecimal {
    var sum: KBigDecimal = KBigDecimal.Zero
    for (element in this) {
        sum += selector(element)
    }
    return sum
}
