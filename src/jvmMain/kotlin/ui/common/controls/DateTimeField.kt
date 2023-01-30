package ui.common.controls

import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import ui.common.state
import java.time.format.DateTimeFormatter

@Composable
fun DateTimeField(
    value: LocalDateTime,
    onValidValueChange: (LocalDateTime) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
) {

    val formatter = remember { DateTimeFormatter.ofPattern(DatePattern) }
    var dateTimeStr by state(value) { formatter.format(value.toJavaLocalDateTime()) }
    var isDateTimeValid by state { true }

    OutlinedTextField(
        modifier = modifier,
        value = dateTimeStr,
        onValueChange = { newValue ->

            val trimmed = newValue.trim().take(14)

            // If still editing, update textfield, signal error
            if (trimmed.isEmpty() || trimmed.length < 14) {
                isDateTimeValid = false
                dateTimeStr = trimmed
                return@OutlinedTextField
            }

            // if input length is valid, try to parse date

            if (trimmed.toLongOrNull() == null) return@OutlinedTextField

            val date = runCatching { java.time.LocalDateTime.parse(trimmed, formatter).toKotlinLocalDateTime() }

            date.onSuccess { onValidValueChange(it) }
            isDateTimeValid = date.isSuccess
            dateTimeStr = trimmed
        },
        label = label,
        supportingText = supportingText,
        isError = isError || !isDateTimeValid,
        singleLine = true,
        enabled = enabled,
        visualTransformation = {

            var out = ""

            for (i in it.text.indices) {
                out += it.text[i]
                when (i) {
                    in listOf(1, 3) -> out += "/"
                    7 -> out += "  "
                    in listOf(9, 11) -> out += ":"
                }
            }

            TransformedText(
                text = AnnotatedString(out),
                offsetMapping = object : OffsetMapping {

                    override fun originalToTransformed(offset: Int): Int {
                        if (offset <= 1) return offset
                        if (offset <= 3) return offset + 1
                        if (offset <= 7) return offset + 2
                        if (offset <= 9) return offset + 4
                        if (offset <= 11) return offset + 5
                        if (offset <= 13) return offset + 6
                        return 20
                    }

                    override fun transformedToOriginal(offset: Int): Int {
                        if (offset <= 2) return offset
                        if (offset <= 5) return offset - 1
                        if (offset <= 10) return offset - 2
                        if (offset <= 11) return offset - 3
                        if (offset <= 14) return offset - 4
                        if (offset <= 17) return offset - 5
                        if (offset <= 19) return offset - 6
                        return 14
                    }
                }
            )
        }
    )
}

private const val DatePattern = "ddMMyyyyHHmmss"
