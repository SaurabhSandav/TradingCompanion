package com.saurabhsandav.core.ui.common.controls

import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.then
import com.saurabhsandav.core.ui.common.form.ValidationScope
import com.saurabhsandav.core.ui.common.form.reportInvalid
import com.saurabhsandav.core.ui.common.trim
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format

object TimeFieldDefaults {

    private val Format = LocalTime.Format {
        hour()
        minute()
        second()
    }

    fun format(value: LocalTime): String = value.format(Format)

    fun parse(value: String): LocalTime = LocalTime.parse(value.padEnd(6, '0'), Format)

    fun parseOrNull(value: String): LocalTime? {
        if (value.isEmpty()) return null
        return runCatching { parse(value) }.getOrNull()
    }

    val InputTransformation = androidx.compose.foundation.text.input.InputTransformation
        .trim()
        .then {
            if (length > 6) delete(6, length)
            if (toString().toIntOrNull() == null) revertAllChanges()
        }

    val OutputTransformation = OutputTransformation {
        if (length > 2) insert(2, ":")
        if (length > 5) insert(5, ":")
    }

    context(_: ValidationScope)
    fun String.validate(): LocalTime? {

        val time = parseOrNull(this)

        if (time == null) reportInvalid("Invalid time")

        return time
    }
}
