package ui.common.form

class FormManager {

    private val controls = mutableListOf<FormState>()

    fun textFieldState(
        initial: String,
        isErrorCheck: (String) -> Boolean,
        onValueChange: MutableFieldState<String>.(String) -> Unit = { setValue(it) },
    ): TextFieldState {
        return TextFieldState(initial, isErrorCheck, onValueChange).also { controls.add(it) }
    }

    fun switchState(
        initial: Boolean,
        onCheckedChange: MutableFieldState<Boolean>.(Boolean) -> Unit = { setValue(it) },
    ): SwitchState {
        return SwitchState(initial, onCheckedChange).also { controls.add(it) }
    }

    fun singleSelectionState(
        initial: String? = null,
        isRequired: Boolean = true,
        onSelectionChange: MutableFieldState<String>.(String) -> Unit = { setValue(it) },
    ): SingleSelectionState {
        return SingleSelectionState(initial, isRequired, onSelectionChange).also { controls.add(it) }
    }

    fun customState(formState: FormState) {
        controls.add(formState)
    }

    // Calls isValid() on all FormStates in case error states are not up-to-date
    fun isFormValid() = controls.map { it.isValid() }.all { it }
}
