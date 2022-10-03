package ui.common.form

class FormManager {

    private val controls = mutableListOf<FormState>()

    fun textFieldState(
        initialValue: String,
        isErrorCheck: (String) -> Boolean,
        onValueChange: (prev: String, new: String) -> String = { _, new -> new },
    ): TextFieldState {
        return TextFieldState(initialValue, isErrorCheck, onValueChange).also { controls.add(it) }
    }

    fun switchState(
        initialValue: Boolean,
        onCheckedChange: (new: Boolean) -> Boolean = { new -> new },
    ): SwitchState {
        return SwitchState(initialValue, onCheckedChange).also { controls.add(it) }
    }

    fun singleSelectionState(
        labelText: String,
        onSelectionChange: (new: String) -> String = { new -> new },
    ): SingleSelectionState {
        return SingleSelectionState(labelText, onSelectionChange).also { controls.add(it) }
    }

    // Calls isValid() on all FormStates in case error states are not up-to-date
    fun isFormValid() = controls.map { it.isValid() }.all { it }
}
