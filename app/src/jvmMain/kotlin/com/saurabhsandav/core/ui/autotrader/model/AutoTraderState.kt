package com.saurabhsandav.core.ui.autotrader.model

import androidx.compose.runtime.*
import com.saurabhsandav.core.trades.model.AutoTraderScriptId
import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.ui.common.form.validations.isRequired
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.datetime.*

@Immutable
internal data class AutoTraderState(
    val configFormModel: ConfigFormModel,
    val scripts: ImmutableList<Script>,
    val scriptFormModel: ScriptFormModel?,
    val isScriptRunning: Boolean,
    val eventSink: (AutoTraderEvent) -> Unit,
) {

    @Immutable
    data class Script(
        val id: AutoTraderScriptId,
        val title: String,
        val description: String,
    )
}

@Stable
internal class ConfigFormModel(
    coroutineScope: CoroutineScope,
    initial: Initial,
) {

    val validator = FormValidator(coroutineScope)

    val tickerField = validator.addField(initial.ticker) { isRequired() }

    val intervalField = validator.addField(initial.interval)

    val titleField = validator.addField(initial.title) { isRequired() }

    class Initial(
        val ticker: String? = "NTPC",
        val title: String = "",
    ) {

        val interval: ClosedRange<LocalDate>

        init {

            val tz = TimeZone.currentSystemDefault()
            val to = Clock.System.now().toLocalDateTime(tz).date
            val from = to.minus(DatePeriod(months = 1))

            interval = from..to
        }
    }
}

@Stable
internal class ScriptFormModel(
    coroutineScope: CoroutineScope,
    isTitleUnique: suspend (String) -> Boolean,
    private val initial: Initial,
) {

    val validator = FormValidator(coroutineScope)

    val id: AutoTraderScriptId
        get() = initial.id

    val titleField = validator.addField(initial.title) {
        isRequired()

        check(
            value = isTitleUnique(this),
            errorMessage = { "Script with name already exists" }
        )
    }

    val descriptionField = validator.addField(initial.description)

    val scriptField = validator.addField(initial.script)

    var consoleText by mutableStateOf("")

    val canSave by derivedStateOf { initial.script != scriptField.value }

    class Initial(
        val id: AutoTraderScriptId,
        val title: String = "",
        val description: String = "",
        val script: String = "",
    )
}
