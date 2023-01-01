package ui.closetradeform

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ui.common.form.FormValidator
import ui.common.form.IsBigDecimal
import ui.common.form.Validation

@Stable
class CloseTradeDetailFormModel(
    validator: FormValidator,
    closeTradeFormModel: CloseTradeFormModel,
    maxFavorableExcursion: String,
    maxAdverseExcursion: String,
    persisted: Boolean,
    tags: List<String>,
) {

    val maxFavorableExcursion = validator.newField(
        initial = maxFavorableExcursion,
        dependsOn = setOf(closeTradeFormModel.isLong, closeTradeFormModel.entry),
        validations = setOf(
            IsBigDecimal,
            Validation(
                errorMessage = "Invalid value",
                validateDependencies = true,
            ) {
                val current = it.toBigDecimal()
                val entryBD = closeTradeFormModel.entry.value.toBigDecimal()
                if (closeTradeFormModel.isLong.value) current >= entryBD else current <= entryBD
            },
        ),
    )

    val maxAdverseExcursion = validator.newField(
        initial = maxAdverseExcursion,
        dependsOn = setOf(closeTradeFormModel.isLong, closeTradeFormModel.entry),
        validations = setOf(
            IsBigDecimal,
            Validation(
                errorMessage = "Invalid value",
                validateDependencies = true,
            ) {
                val current = it.toBigDecimal()
                val entryBD = closeTradeFormModel.entry.value.toBigDecimal()
                if (closeTradeFormModel.isLong.value) current <= entryBD else current >= entryBD
            },
        ),
    )

    var persisted by mutableStateOf(persisted)

    val tags by mutableStateOf(tags)
}
