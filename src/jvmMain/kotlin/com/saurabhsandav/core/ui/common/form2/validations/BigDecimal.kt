package com.saurabhsandav.core.ui.common.form2.validations

import com.saurabhsandav.core.ui.common.form2.Validation
import com.saurabhsandav.core.ui.common.form2.ValidationScope
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
