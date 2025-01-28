package com.saurabhsandav.core.ui.common.form.validations

import com.saurabhsandav.core.ui.common.form.ValidationScope
import java.math.BigDecimal

context(ValidationScope)
fun String.isBigDecimal(
    errorMessage: () -> String = { "Not a valid number" },
): BigDecimal? {

    val bdValue = toBigDecimalOrNull()

    if (bdValue == null) reportInvalid(message = errorMessage())

    return bdValue
}

context(ValidationScope)
fun BigDecimal.isPositive(
    errorMessage: () -> String = { "Cannot be 0 or negative" },
): BigDecimal {
    if (this < BigDecimal.ZERO) reportInvalid(message = errorMessage())
    return this
}
