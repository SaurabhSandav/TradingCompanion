package ui.pnlcalculator

import androidx.compose.runtime.*
import ui.common.form.FormValidator
import ui.common.form.IsBigDecimal
import ui.common.form.IsInt
import ui.common.form.Validation
import ui.common.form.fields.switch
import ui.common.form.fields.textField

@Stable
internal class PNLCalculatorModel(
    validator: FormValidator,
    quantity: String,
    isLong: Boolean,
    entry: String,
    exit: String,
) {

    var enableModification by mutableStateOf(true)

    val quantity = validator.textField(
        initial = quantity,
        validations = setOf(
            IsInt,
            Validation("Cannot be 0 or negative") { it.toInt() > 0 },
        ),
    )

    val isLong = validator.switch(isLong)

    val entry = validator.textField(
        initial = entry,
        validations = setOf(IsBigDecimal),
    )

    val exit = validator.textField(
        initial = exit,
        validations = setOf(IsBigDecimal),
    )

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
