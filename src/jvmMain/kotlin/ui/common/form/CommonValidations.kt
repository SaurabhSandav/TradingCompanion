package ui.common.form

val IsNotNull = Validation<String?>("Required") { it != null }

val IsNotEmpty = Validation<String>("Cannot be empty") { it.isNotEmpty() }

val IsBigDecimal = Validation<String>("Not a valid number") { it.toBigDecimalOrNull() != null }

val IsInt = Validation<String>("Not a valid integer") { it.toIntOrNull() != null }
