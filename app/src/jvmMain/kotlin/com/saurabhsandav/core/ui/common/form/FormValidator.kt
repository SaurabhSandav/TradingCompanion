package com.saurabhsandav.core.ui.common.form

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Composable
fun rememberFormValidator(
    formModels: List<FormModel>,
    onSubmit: (() -> Unit)? = null,
): FormValidator {

    val scope = rememberCoroutineScope()
    val validator = remember { FormValidator(scope, onSubmit) }

    SideEffect {
        validator.onSubmit = onSubmit
    }

    DisposableEffect(formModels) {
        formModels.forEach(validator::addModel)
        onDispose {
            formModels.forEach(validator::removeModel)
        }
    }

    return validator
}

class FormValidator(
    private val coroutineScope: CoroutineScope,
    internal var onSubmit: (() -> Unit)? = null,
) {

    private val formModels = mutableStateListOf<FormModel>()
    private val fields
        get() = formModels.flatMap { it.fields }

    private val submitMutex = Mutex()

    val isValid: Boolean by derivedStateOf { fields.all { it.isValid } }

    private var enableSubmit by mutableStateOf(true)

    val canSubmit by derivedStateOf { enableSubmit && isValid }

    internal fun addModel(formModel: FormModel) {
        formModels.add(formModel)
        formModel.onAttach(coroutineScope)
    }

    internal fun removeModel(formModel: FormModel) {
        formModels.remove(formModel)
        formModel.onDetach()
    }

    suspend fun validate(): Boolean = fields.map { it.validate() }.all { it }

    fun submit() = coroutineScope.launchUnit {

        if (submitMutex.isLocked) return@launchUnit

        submitMutex.withLock {

            enableSubmit = false

            val isValid = validate()

            if (isValid) onSubmit?.let { it() }

            enableSubmit = true
        }
    }
}
