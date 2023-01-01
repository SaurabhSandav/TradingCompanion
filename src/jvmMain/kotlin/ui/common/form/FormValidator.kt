package ui.common.form

class FormValidator {

    private val fields = mutableListOf<ValidatableState<*>>()

    fun <T> newField(
        initial: T,
        validations: Set<Validation<T>> = emptySet(),
        dependsOn: Set<ValidatableState<*>> = emptySet(),
    ): ValidatableState<T> {

        val state = ValidatableState(
            initial = initial,
            validations = validations,
            dependsOn = dependsOn,
        )

        fields.add(state)

        dependsOn.forEach { it.registerDependent(state) }

        return state
    }

    fun removeField(state: ValidatableState<*>) {

        state.dependsOn.forEach { it.unregisterDependent(state) }

        fields.remove(state)
    }

    fun isValid(): Boolean {
        return fields.onEach { it.validate() }.all { it.isValid }
    }
}
