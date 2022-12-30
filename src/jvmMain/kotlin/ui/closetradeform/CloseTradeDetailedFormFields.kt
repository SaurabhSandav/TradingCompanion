package ui.closetradeform

import ui.common.form.FormScope
import ui.common.form.switchState
import ui.common.form.textFieldState

internal class CloseTradeDetailedFormFields(
    private val formScope: FormScope,
    private val initial: Model,
) {

    val maxFavorableExcursion = formScope.textFieldState(
        initial = initial.maxFavorableExcursion,
        isErrorCheck = { it.isNotEmpty() && it.toBigDecimalOrNull() == null },
        onValueChange = { setValue(it.trim()) },
    )

    val maxAdverseExcursion = formScope.textFieldState(
        initial = initial.maxAdverseExcursion,
        isErrorCheck = { it.isNotEmpty() && it.toBigDecimalOrNull() == null },
        onValueChange = { setValue(it.trim()) },
    )

    val persisted = formScope.switchState(initial.persisted)

    fun getModelIfValidOrNull(): Model? = if (!formScope.isFormValid()) null else Model(
        id = initial.id,
        maxFavorableExcursion = maxFavorableExcursion.value,
        maxAdverseExcursion = maxAdverseExcursion.value,
        tags = initial.tags,
        persisted = persisted.value,
    )

    class Model(
        val id: Long,
        val maxFavorableExcursion: String,
        val maxAdverseExcursion: String,
        val tags: List<String>,
        val persisted: Boolean,
    )
}
