package com.saurabhsandav.core.ui.common.form.adapter

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import com.saurabhsandav.core.ui.common.form.FormField
import com.saurabhsandav.core.ui.common.form.FormModel
import com.saurabhsandav.core.ui.common.form.Validation
import kotlinx.coroutines.flow.Flow

class MutableStateFormFieldAdapter<V> : FormField.Adapter<MutableState<V>, V> {

    override fun getValue(holder: MutableState<V>): V = holder.value

    override fun getFlow(holder: MutableState<V>): Flow<V> = snapshotFlow { holder.value }
}

@Suppress("FunctionName")
fun <V> MutableStateFormField(
    initial: V,
    validation: Validation<V>?,
): FormField<MutableState<V>, V> {
    val state = mutableStateOf(initial)
    val adapter = MutableStateFormFieldAdapter<V>()
    return FormField(state, adapter, validation)
}

fun <V> FormModel.addMutableStateField(
    initial: V,
    validation: Validation<V>? = null,
): FormField<MutableState<V>, V> {
    val field = MutableStateFormField(initial, validation)
    return addField(field)
}
