package com.saurabhsandav.core.ui.common.form.adapter

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.TextRange
import com.saurabhsandav.core.ui.common.form.FormField
import com.saurabhsandav.core.ui.common.form.FormModel
import com.saurabhsandav.core.ui.common.form.Validation
import kotlinx.coroutines.flow.Flow

object TextFieldStateFormFieldAdapter : FormField.Adapter<TextFieldState, String> {

    override fun getValue(holder: TextFieldState): String = holder.text.toString()

    override fun getFlow(holder: TextFieldState): Flow<String> = snapshotFlow { holder.text.toString() }
}

@Suppress("FunctionName")
fun TextFieldStateFormField(
    initialText: String = "",
    initialSelection: TextRange = TextRange(initialText.length),
    validation: Validation<String>?,
): FormField<TextFieldState, String> {
    val state = TextFieldState(initialText, initialSelection)
    val adapter = TextFieldStateFormFieldAdapter
    return FormField(state, adapter, validation)
}

fun FormModel.addTextFieldStateField(
    initialText: String = "",
    initialSelection: TextRange = TextRange(initialText.length),
    validation: Validation<String>? = null,
): FormField<TextFieldState, String> {
    val field = TextFieldStateFormField(initialText, initialSelection, validation)
    return addField(field)
}
