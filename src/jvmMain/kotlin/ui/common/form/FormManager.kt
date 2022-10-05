package ui.common.form

import androidx.compose.runtime.saveable.Saver

class FormManager(
    private val restored: Any? = null,
) {

    private val controls = mutableMapOf<FormState, Saver<FormState, Any>>()

    fun <T : FormState> controlState(saver: Saver<T, Any>, initial: () -> T): T {

        val restoredControl = restored?.let { saver.restore(it) }

        @Suppress("UNCHECKED_CAST")
        return (restoredControl ?: initial()).also { controls[it] = saver as Saver<FormState, Any> }
    }

    // Calls isValid() on all FormStates in case error states are not up-to-date
    fun isFormValid() = controls.map { it.key.isValid() }.all { it }
}
