package ui.common.form

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.*

class FormScope private constructor(
    private val restored: List<Any?>?,
) {

    constructor() : this(null)

    private val controls = mutableMapOf<FormState<*>, Saver<FormState<*>, Any>>()

    fun <T : FormState<*>> controlState(saver: Saver<T, Any>, initial: () -> T): T {

        val restoredControl = restored?.get(controls.size)?.let { saver.restore(it) }

        @Suppress("UNCHECKED_CAST")
        return (restoredControl ?: initial()).also { controls[it] = saver as Saver<FormState<*>, Any> }
    }

    fun isFormValid() = controls.onEach { it.key.validate() }.all { !it.key.isError }

    companion object {

        val Saver = listSaver(
            save = { formScope ->
                formScope.controls.map {
                    with(it.value) {
                        save(it.key)
                    }
                }
            },
            restore = { FormScope(it) },
        )
    }
}

@Composable
fun rememberFormScope(): FormScope = rememberSaveable(saver = FormScope.Saver) { FormScope() }
