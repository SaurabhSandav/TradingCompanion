package com.saurabhsandav.core.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.input.then
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import com.saurabhsandav.core.ui.common.app.AppDialog
import com.saurabhsandav.core.ui.theme.dimens
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ColorPickerDialog(
    onDismissRequest: () -> Unit,
    onColorSelected: (Color) -> Unit,
    initialSelection: Color? = null,
) {

    AppDialog(
        onDismissRequest = onDismissRequest,
        size = MaterialTheme.dimens.dialogSize,
    ) {

        Column(
            modifier = Modifier.padding(MaterialTheme.dimens.containerPadding),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.columnVerticalSpacing),
        ) {

            val controller = rememberColorPickerController()
            val selectedColor by controller.selectedColor
            val selectedColorHex = rememberTextFieldState(
                initialText = remember { controller.selectedColor.value.toHexString() },
            )
            var isHexError by state { false }

            HsvColorPicker(
                modifier = Modifier.weight(1F),
                controller = controller,
                initialColor = initialSelection,
            )

            HorizontalDivider()

            LaunchedEffect(controller) {
                snapshotFlow { controller.selectedColor.value }.collectLatest { color ->
                    selectedColorHex.setTextAndPlaceCursorAtEnd(color.toHexString())
                    isHexError = false
                }
            }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                state = selectedColorHex,
                inputTransformation = InputTransformation.trim().then {

                    val color = Color.hex(toString())

                    if (color != null) controller.wheelColor = color
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
                    onDismissRequest()
                },
                content = { Text("Select") },
            )
        }
    }
}
