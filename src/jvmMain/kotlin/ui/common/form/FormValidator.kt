package ui.common.form

class FormValidator {

    private val fields = mutableListOf<FormField<*>>()

    fun <T> addField(field: FormField<T>) {

        fields.add(field)

        field.validations.flatMap { it.dependsOn }.forEach { it.registerDependent(field) }
    }

    fun removeField(field: FormField<*>) {

        field.validations.flatMap { it.dependsOn }.forEach { it.unregisterDependent(field) }

        fields.remove(field)
    }

    fun isValid(): Boolean {
        return fields.onEach { it.validate() }.all { it.isValid }
    }
}
