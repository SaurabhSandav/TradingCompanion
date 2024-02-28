package com.saurabhsandav.core.ui.pnlcalculator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.ui.common.form.validations.isBigDecimal
import com.saurabhsandav.core.ui.common.form.validations.isInt
import com.saurabhsandav.core.ui.common.form.validations.isPositive
import com.saurabhsandav.core.ui.common.form.validations.isRequired

internal class PNLCalculatorModel(
    val validator: FormValidator,
    quantity: String,
    isLong: Boolean,
    entry: String,
    exit: String,
) {

    var enableModification by mutableStateOf(true)

    val quantityField = validator.addField(quantity) {
        isRequired()
        isInt {
            isPositive()
        }
    }

    val isLongField = validator.addField(isLong)

    val entryField = validator.addField(entry) {
        isRequired()
        isBigDecimal {
            isPositive()
        }
    }

    val exitField = validator.addField(exit) {
        isRequired()
        isBigDecimal {

            isPositive()

            check(
                value = validated(entryField).toBigDecimal().compareTo(this) != 0,
                errorMessage = { "Entry and Exit cannot be the same" },
            )
        }
    }

    val pnlEntries = mutableStateListOf<PNLEntry>()
}

class PNLEntry(
    val id: Int,
    val side: String,
    val quantity: String,
    val entry: String,
    val exit: String,
    val breakeven: String,
    val pnl: String,
    val isProfitable: Boolean,
    val charges: String,
    val netPNL: String,
    val isNetProfitable: Boolean,
    val isRemovable: Boolean = false,
)
