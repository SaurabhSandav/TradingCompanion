package com.saurabhsandav.core.ui.common.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import com.saurabhsandav.core.LocalDensityFraction

@Composable
fun AppWindow(
    onCloseRequest: () -> Unit,
    state: WindowState = rememberWindowState(),
    visible: Boolean = true,
    title: String? = null,
    icon: Painter? = null,
    undecorated: Boolean = false,
    transparent: Boolean = false,
    resizable: Boolean = true,
    enabled: Boolean = true,
    focusable: Boolean = true,
    alwaysOnTop: Boolean = false,
    onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
    onKeyEvent: (KeyEvent) -> Boolean = { false },
    content: @Composable FrameWindowScope.() -> Unit,
) {

    val appWindowState = remember(state) { (state as? AppWindowState) ?: AppWindowState(state) }

    Window(
        onCloseRequest = onCloseRequest,
        state = state,
        visible = visible,
        title = title ?: appWindowState.title,
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

        SideEffect { appWindowState.setWindow(window) }

        val density = LocalDensity.current
        val densityFraction = LocalDensityFraction.current

        val newDensity = Density(density.density * densityFraction, density.fontScale)

        CompositionLocalProvider(
            LocalDensity provides newDensity,
            LocalAppWindowState provides appWindowState,
        ) {

            Surface(
                modifier = Modifier.fillMaxSize(),
                content = { content() },
            )
        }
    }
}

@Composable
fun rememberAppWindowState(
    windowState: WindowState = rememberWindowState(),
    defaultTitle: String = "Untitled",
): AppWindowState = remember { AppWindowState(windowState, defaultTitle) }

@Composable
fun WindowTitle(title: String) {

    val appWindowState = LocalAppWindowState.current
    // Copied from rememberSaveable implementation
    val id = currentCompositeKeyHash.toString(36)

    // Set window title
    DisposableEffect(appWindowState) {
        appWindowState.setCompositionTitle(id, title)
        onDispose { appWindowState.removeCompositionTitle(id) }
    }
}

@Stable
class AppWindowState(
    windowState: WindowState,
    private val defaultTitle: String = "Untitled",
) : WindowState by windowState {

    private lateinit var window: ComposeWindow
    private val titles = ArrayDeque<Pair<String, String>>()

    var title by mutableStateOf(defaultTitle)
        private set

    internal fun setCompositionTitle(id: String, titleText: String) {

        titles.addLast(id to titleText)

        title = titles.lastOrNull()?.second ?: defaultTitle
    }

    internal fun removeCompositionTitle(id: String) {

        val iterator = titles.listIterator()

        while (iterator.hasPrevious()) {
            if (iterator.previous().first == id) {
                iterator.remove()
                break
            }
        }

        title = titles.lastOrNull()?.second ?: defaultTitle
    }

    internal fun setWindow(window: ComposeWindow) {
        this.window = window
    }

    fun toFront() {
        window.toFront()
    }

    fun toBack() {
        window.toBack()
    }
}

val LocalAppWindowState = staticCompositionLocalOf<AppWindowState> { error("AppWindowState not set") }
