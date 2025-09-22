package com.saurabhsandav.core.ui.common.controls

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.saurabhsandav.core.ui.common.form.ValidationScope
import com.saurabhsandav.core.ui.common.form.reportInvalid
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

    fun onValueChange(block: (String) -> Unit): (String) -> Unit = returnBlock@{ newValue ->
        val trimmed = newValue.trim().take(6)
        if (trimmed.toIntOrNull() == null) return@returnBlock
        block(trimmed)
    }

    val VisualTransformation = VisualTransformation {

        var out = ""

        for (i in it.text.indices) {
            out += it.text[i]
            if (i in listOf(1, 3)) out += ":"
        }

        TransformedText(
            text = AnnotatedString(out),
            offsetMapping = object : OffsetMapping {

                override fun originalToTransformed(offset: Int): Int {
                    if (offset <= 1) return offset
                    if (offset <= 3) return offset + 1
                    if (offset <= 5) return offset + 2
                    return 8
                }

                override fun transformedToOriginal(offset: Int): Int {
                    if (offset <= 2) return offset
                    if (offset <= 5) return offset - 1
                    if (offset <= 8) return offset - 2
                    return 6
                }
            },
        )
    }

    context(_: ValidationScope)
    fun String.validate(): LocalTime? {

        val time = parseOrNull(this)

        if (time == null) reportInvalid("Invalid time")

        return time
    }
}
