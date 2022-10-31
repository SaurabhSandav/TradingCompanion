package ui.addclosedtradedetailed

import kotlinx.datetime.LocalDateTime
import ui.common.form.*

internal class CloseTradeDetailedFormFields(
    private val formScope: FormScope,
    private val initial: Model,
) {

    val ticker = formScope.singleSelectionState(initial.ticker)

    val quantity = formScope.textFieldState(
        initial = initial.quantity,
        isErrorCheck = { it.isEmpty() || it.toBigDecimalOrNull() == null },
        onValueChange = { setValue(it.trim()) },
    )

    val isLong = formScope.switchState(initial.isLong)

    val entry = formScope.textFieldState(
        initial = initial.entry,
        isErrorCheck = { it.isEmpty() || it.toBigDecimalOrNull() == null },
        onValueChange = { setValue(it.trim()) },
    )

    val stop = formScope.textFieldState(
        initial = initial.stop,
        isErrorCheck = { it.isNotEmpty() && it.toBigDecimalOrNull() == null },
        onValueChange = { setValue(it.trim()) },
    )

    val entryDateTime = formScope.dateTimeFieldState(initial.entryDateTime)

    val target = formScope.textFieldState(
        initial = initial.target,
        isErrorCheck = { it.isNotEmpty() && it.toBigDecimalOrNull() == null },
        onValueChange = { setValue(it.trim()) },
    )

    val exit = formScope.textFieldState(
        initial = initial.exit,
        isErrorCheck = { it.isEmpty() || it.toBigDecimalOrNull() == null },
        onValueChange = { setValue(it.trim()) },
    )

    val exitDateTime = formScope.dateTimeFieldState(initial.exitDateTime)

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
        ticker = ticker.value,
        quantity = quantity.value,
        isLong = isLong.value,
        entry = entry.value,
        stop = stop.value,
        entryDateTime = entryDateTime.value,
        target = target.value,
        exit = exit.value,
        exitDateTime = exitDateTime.value,
        maxFavorableExcursion = maxFavorableExcursion.value,
        maxAdverseExcursion = maxAdverseExcursion.value,
        tags = initial.tags,
        persisted = persisted.value,
    )

    class Model(
        val id: Long,
        val ticker: String?,
        val quantity: String,
        val isLong: Boolean,
        val entry: String,
        val stop: String,
        val entryDateTime: LocalDateTime,
        val target: String,
        val exit: String,
        val exitDateTime: LocalDateTime,
        val maxFavorableExcursion: String,
        val maxAdverseExcursion: String,
        val tags: List<String>,
        val persisted: Boolean,
    )
}
