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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate
import ui.common.state
import java.time.format.DateTimeFormatter

@Composable
fun DateField(
    value: LocalDate,
    onValidValueChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
) {

    val formatter = remember { DateTimeFormatter.ofPattern(DatePattern) }
    var dateStr by state(value) { formatter.format(value.toJavaLocalDate()) }
    var isDateValid by state { true }

    OutlinedTextField(
        modifier = modifier,
        value = dateStr,
        onValueChange = { newValue ->

            val trimmed = newValue.trim().take(8)

            // If still editing, update textfield, signal error
            if (trimmed.isEmpty() || trimmed.length < 8) {
                isDateValid = false
                dateStr = trimmed
                return@OutlinedTextField
            }

            // if input length is valid, try to parse date

            if (trimmed.toIntOrNull() == null) return@OutlinedTextField

            val date = runCatching { java.time.LocalDate.parse(trimmed, formatter).toKotlinLocalDate() }

            date.onSuccess { onValidValueChange(it) }
            isDateValid = date.isSuccess
            dateStr = trimmed
        },
        label = label,
        isError = isError || !isDateValid,
        singleLine = true,
        enabled = enabled,
        visualTransformation = {

            var out = ""

            for (i in it.text.indices) {
                out += it.text[i]
                if (i in listOf(1, 3)) out += "/"
            }

            TransformedText(
                text = AnnotatedString(out),
                offsetMapping = object : OffsetMapping {

                    override fun originalToTransformed(offset: Int): Int {
                        if (offset <= 1) return offset
                        if (offset <= 3) return offset + 1
                        if (offset <= 7) return offset + 2
                        return 10
                    }

                    override fun transformedToOriginal(offset: Int): Int {
                        if (offset <= 2) return offset
                        if (offset <= 5) return offset - 1
                        if (offset <= 10) return offset - 2
                        return 8
                    }
                }
            )
        }
    )
}

private const val DatePattern = "ddMMyyyy"
