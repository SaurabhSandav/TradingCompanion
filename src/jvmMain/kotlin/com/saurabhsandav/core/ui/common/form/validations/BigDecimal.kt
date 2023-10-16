package com.saurabhsandav.core.ui.common.form.validations

import com.saurabhsandav.core.ui.common.form.Validation
import com.saurabhsandav.core.ui.common.form.ValidationScope
import java.math.BigDecimal

context(ValidationScope)
suspend fun String.isBigDecimal(
    validation: Validation<BigDecimal>? = null,
) {

    ifBlank { return }

    val bdValue = toBigDecimalOrNull()

    bdValue.isRequired { "Not a valid number" }

    validation?.apply {
        bdValue.validate()
    }
}

context(ValidationScope)
fun BigDecimal.isPositive() {
    check(this > BigDecimal.ZERO) { "Cannot be 0 or negative" }
}
