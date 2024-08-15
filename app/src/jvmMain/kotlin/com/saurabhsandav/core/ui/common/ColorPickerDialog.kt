package com.saurabhsandav.core.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.text.style.TextAlign
import com.godaddy.android.colorpicker.ClassicColorPicker
import com.godaddy.android.colorpicker.HsvColor
import com.saurabhsandav.core.ui.common.app.AppDialogWindow
import com.saurabhsandav.core.ui.theme.dimens

@Composable
fun ColorPickerDialog(
    onCloseRequest: () -> Unit,
    onColorSelected: (Color) -> Unit,
    initialSelection: Color? = null,
) {

    AppDialogWindow(
        onCloseRequest = onCloseRequest,
        title = "Select color",
        onKeyEvent = {

            when (it.key) {
                Key.Escape -> {
                    onCloseRequest()
                    true
                }

                else -> false
            }
        }
    ) {

        Column(
            modifier = Modifier.padding(MaterialTheme.dimens.containerPadding),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.columnVerticalSpacing),
        ) {

            var selectedColor by state { initialSelection ?: HsvColor.DEFAULT.toColor() }
            var selectedColorHex by state { selectedColor.toHexString() }
            var isHexError by state { false }

            ClassicColorPicker(
                modifier = Modifier.weight(1F),
                color = HsvColor.from(color = selectedColor),
                onColorChanged = { color ->
                    selectedColor = color.toColor()
                    selectedColorHex = selectedColor.toHexString()
                },
                showAlphaBar = false,
            )

            HorizontalDivider()

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = selectedColorHex,
                onValueChange = { text ->

                    selectedColorHex = text

                    val color = Color.fromStringOrNull(text)

                    color?.let { selectedColor = it }
                    isHexError = color == null
                },
                isError = isHexError,
                label = { Text("Hex") },
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
            )

            HorizontalDivider()

            Box(
                modifier = Modifier.fillMaxWidth()
                    .height(ButtonDefaults.MinHeight)
                    .drawBehind { drawRect(selectedColor) },
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onColorSelected(selectedColor)
                    onCloseRequest()
                },
                content = { Text("Select") },
            )
        }
    }
}
