package com.saurabhsandav.core.ui.common.form.validations

import com.saurabhsandav.core.ui.common.form.ValidationScope
import com.saurabhsandav.core.ui.common.form.reportInvalid
import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.kbigdecimal.toKBigDecimalOrNull

context(_: ValidationScope)
fun String.isBigDecimal(errorMessage: () -> String = { "Not a valid number" }): KBigDecimal? {

    val bdValue = toKBigDecimalOrNull()

    if (bdValue == null) reportInvalid(message = errorMessage())

    return bdValue
}

context(_: ValidationScope)
fun KBigDecimal.isPositive(errorMessage: () -> String = { "Cannot be 0 or negative" }): KBigDecimal {
    if (this <= KBigDecimal.Zero) reportInvalid(message = errorMessage())
    return this
}
