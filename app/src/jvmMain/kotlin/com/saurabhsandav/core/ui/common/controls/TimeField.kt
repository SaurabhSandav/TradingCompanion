package com.saurabhsandav.core.ui.common.controls

import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import com.saurabhsandav.core.ui.common.state
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format

@Composable
fun TimeField(
    value: LocalTime?,
    onValidValueChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
) {

    var timeText by state(value) { value?.format(TimeFormat) ?: "" }
    var isTimeValid by state { true }

    OutlinedTextField(
        modifier = modifier,
        value = timeText,
        onValueChange = { newValue ->

            val trimmed = newValue.trim().take(6)

            // If still editing, update textfield, signal error
            if (trimmed.isEmpty() || trimmed.length < 6) {
                isTimeValid = false
                timeText = trimmed
                return@OutlinedTextField
            }

            // if input length is valid, try to parse time

            if (trimmed.toIntOrNull() == null) return@OutlinedTextField

            val time = runCatching { LocalTime.parse(trimmed, TimeFormat) }

            time.onSuccess { onValidValueChange(it) }
            isTimeValid = time.isSuccess
            timeText = trimmed
        },
        enabled = enabled,
        label = label,
        trailingIcon = trailingIcon,
        supportingText = supportingText,
        isError = isError || !isTimeValid,
        singleLine = true,
        visualTransformation = {

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
        },
    )
}

private val TimeFormat = LocalTime.Format {
    hour()
    minute()
    second()
}
