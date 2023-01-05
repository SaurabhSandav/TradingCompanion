package ui.common.form

val IsBigDecimal = Validation<String>("Not a valid number") { it.toBigDecimalOrNull() != null }

val IsInt = Validation<String>("Not a valid integer") { it.toIntOrNull() != null }
