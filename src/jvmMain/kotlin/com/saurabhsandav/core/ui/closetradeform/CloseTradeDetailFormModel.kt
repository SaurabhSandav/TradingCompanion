package com.saurabhsandav.core.ui.closetradeform

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.saurabhsandav.core.TradeTag
import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.ui.common.form.IsBigDecimal
import com.saurabhsandav.core.ui.common.form.Validation
import com.saurabhsandav.core.ui.common.form.fields.textField

@Stable
class CloseTradeDetailFormModel(
    validator: FormValidator,
    closeTradeFormModel: CloseTradeFormModel,
    maxFavorableExcursion: String,
    maxAdverseExcursion: String,
    persisted: Boolean,
    notes: String,
) {

    val maxFavorableExcursion = validator.textField(
        initial = maxFavorableExcursion,
        isRequired = false,
        validations = setOf(
            IsBigDecimal,
            Validation(
                errorMessage = "Invalid value",
                dependsOn = setOf(closeTradeFormModel.isLong, closeTradeFormModel.entry),
            ) {
                val current = it.toBigDecimal()
                val entryBD = closeTradeFormModel.entry.value.toBigDecimal()
                if (closeTradeFormModel.isLong.value) current >= entryBD else current <= entryBD
            },
        ),
    )

    val maxAdverseExcursion = validator.textField(
        initial = maxAdverseExcursion,
        isRequired = false,
        validations = setOf(
            IsBigDecimal,
            Validation(
                errorMessage = "Invalid value",
                dependsOn = setOf(closeTradeFormModel.isLong, closeTradeFormModel.entry),
            ) {
                val current = it.toBigDecimal()
                val entryBD = closeTradeFormModel.entry.value.toBigDecimal()
                if (closeTradeFormModel.isLong.value) current <= entryBD else current >= entryBD
            },
        ),
    )

    var persisted by mutableStateOf(persisted)

    var tags by mutableStateOf<List<TradeTag>>(emptyList())

    val notes = validator.textField(
        initial = notes,
        isRequired = false,
    )
}
