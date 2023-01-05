package ui.pnlcalculator

import androidx.compose.runtime.*
import ui.common.form.*

@Stable
internal class PNLCalculatorModel(
    validator: FormValidator,
    quantity: String,
    isLong: Boolean,
    entry: String,
    exit: String,
) {

    var enableModification by mutableStateOf(true)

    val quantity = validator.newField(
        initial = quantity,
        validations = setOf(
            IsNotEmpty,
            IsInt,
            Validation("Cannot be 0 or negative") { it.toInt() > 0 },
        ),
    )

    val isLong = validator.newField(isLong)

    val entry = validator.newField(
        initial = entry,
        validations = setOf(IsNotEmpty, IsBigDecimal),
    )

    val exit = validator.newField(
        initial = exit,
        dependsOn = setOf(this.isLong, this.entry),
        validations = setOf(
            IsNotEmpty,
            IsBigDecimal,
        ),
    )

    val pnlEntries = mutableStateListOf<PNLEntry>()
}

class PNLEntry(
    val price: String,
    val pnl: String,
    val isProfitable: Boolean,
    val netPNL: String,
    val isNetProfitable: Boolean,
    val isRemovable: Boolean = false,
)
