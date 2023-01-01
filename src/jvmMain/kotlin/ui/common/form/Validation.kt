package ui.common.form

class Validation<T>(
    val errorMessage: String,
    val validateDependencies: Boolean = false,
    val isValid: (T) -> Boolean,
)
