package com.saurabhsandav.core.ui.tagform

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import com.saurabhsandav.core.LocalScreensModule
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.common.ColorPickerDialog
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.app.rememberAppWindowState
import com.saurabhsandav.core.ui.common.form.isError
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.tagform.model.TagFormModel
import com.saurabhsandav.core.ui.tagform.model.TagFormType
import com.saurabhsandav.core.ui.theme.dimens

@Composable
fun TagFormWindow(
    profileId: ProfileId,
    formType: TagFormType,
    onCloseRequest: () -> Unit,
) {

    val scope = rememberCoroutineScope()
    val screensModule = LocalScreensModule.current
    val presenter = remember { screensModule.tagFormModule(scope).presenter(onCloseRequest, profileId, formType) }
    val state by presenter.state.collectAsState()

    val windowState = rememberAppWindowState(
        size = DpSize(width = 250.dp, height = 300.dp),
        preferredPlacement = WindowPlacement.Floating,
        forcePreferredPlacement = true,
    )

    AppWindow(
        onCloseRequest = onCloseRequest,
        state = windowState,
        title = state.title,
    ) {

        when (val model = state.formModel) {
            null -> CircularProgressIndicator(Modifier.fillMaxSize().wrapContentSize())
            else -> Form(model = model)
        }
    }
}

@Composable
private fun Form(
    model: TagFormModel,
) {

    Column(
        modifier = Modifier.fillMaxSize().padding(MaterialTheme.dimens.containerPadding),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        val initialFocusRequester = remember { FocusRequester() }
        var showColorPicker by state { false }

        LaunchedEffect(Unit) { initialFocusRequester.requestFocus() }

        OutlinedTextField(
            modifier = Modifier.focusRequester(initialFocusRequester),
            value = model.nameField.value,
            onValueChange = { model.nameField.value = it },
            label = { Text("Name") },
            isError = model.nameField.isError,
            supportingText = model.nameField.errorMessage?.let { { Text(it) } },
            singleLine = true,
        )

        OutlinedTextField(
            value = model.descriptionField.value,
            onValueChange = { model.descriptionField.value = it },
            label = { Text("Description") },
        )

        AnimatedContent(
            targetState = model.colorField.value,
            contentKey = { it == null },
        ) { targetColor ->

            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.rowHorizontalSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {

                if (targetColor != null) {

                    val animatedColor by animateColorAsState(targetColor)

                    Box(
                        modifier = Modifier
                            .weight(1F)
                            .height(ButtonDefaults.MinHeight)
                            .drawBehind { drawRect(animatedColor) }
                            .clickable { showColorPicker = true },
                    )

                    IconButton(onClick = { model.colorField.value = null }) {

                        Icon(Icons.Default.Close, contentDescription = "Remove color")
                    }
                } else {

                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showColorPicker = true },
                        content = { Text("Select color") },
                    )
                }
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = model.validator::submit,
            enabled = model.validator.canSubmit,
            content = { Text("Save") },
        )

        if (showColorPicker) {

            ColorPickerDialog(
                onCloseRequest = { showColorPicker = false },
                onColorSelected = { model.colorField.value = it },
                initialSelection = model.colorField.value,
            )
        }
    }
}
