package com.saurabhsandav.core.ui.common.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.DialogState
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.DialogWindowScope
import androidx.compose.ui.window.rememberDialogState
import com.saurabhsandav.core.LocalAppConfig
import com.saurabhsandav.core.adjustedDensity

@Composable
fun AppDialogWindow(
    onCloseRequest: () -> Unit,
    state: DialogState = rememberDialogState(),
    visible: Boolean = true,
    title: String = "Untitled",
    icon: Painter? = null,
    undecorated: Boolean = false,
    transparent: Boolean = false,
    resizable: Boolean = true,
    enabled: Boolean = true,
    focusable: Boolean = true,
    alwaysOnTop: Boolean = false,
    onPreviewKeyEvent: ((KeyEvent) -> Boolean) = { false },
    onKeyEvent: ((KeyEvent) -> Boolean) = { false },
    content: @Composable DialogWindowScope.() -> Unit,
) {

    DialogWindow(
        onCloseRequest = onCloseRequest,
        state = state,
        visible = visible,
        title = title,
        icon = icon,
        undecorated = undecorated,
        transparent = transparent,
        resizable = resizable,
        enabled = enabled,
        focusable = focusable,
        alwaysOnTop = alwaysOnTop,
        onPreviewKeyEvent = onPreviewKeyEvent,
        onKeyEvent = onKeyEvent,
    ) {

        val appConfig = LocalAppConfig.current

        CompositionLocalProvider(
            LocalDensity provides appConfig.adjustedDensity(),
        ) {

            Surface(
                modifier = Modifier.fillMaxSize(),
                content = { content() },
            )
        }
    }
}
