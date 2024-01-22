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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import com.saurabhsandav.core.LocalDensityFraction
import kotlinx.coroutines.flow.drop

@Composable
fun AppWindow(
    onCloseRequest: () -> Unit,
    state: AppWindowState = rememberAppWindowState(),
    visible: Boolean = true,
    title: String = "Untitled",
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
    val appWindowOwner = LocalAppWindowOwner.current

    if (appWindowOwner != null) {

        DisposableEffect(Unit) {
            appWindowOwner.registerChild(appWindowState)
            onDispose { appWindowOwner.unRegisterChild(appWindowState) }
        }
    }

    Window(
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

        appWindowState.setWindow(window)

        val density = LocalDensity.current
        val densityFraction = LocalDensityFraction.current

        val newDensity = Density(density.density * densityFraction, density.fontScale)

        CompositionLocalProvider(
            LocalDensity provides newDensity,
            LocalAppWindowState provides appWindowState,
            // AppWindowOwner of this window should not operate on child windows
            LocalAppWindowOwner provides null,
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
    preferredPlacement: WindowPlacement? = null,
    forcePreferredPlacement: Boolean = false,
    size: DpSize = DpSize(800.dp, 600.dp),
    defaultTitle: String? = null,
    titleTransform: ((String) -> String)? = null,
): AppWindowState {

    val windowConfig = LocalAppWindowConfig.current

    val state = remember {

        AppWindowState(
            windowState = WindowState(
                placement = when {
                    forcePreferredPlacement -> preferredPlacement
                    else -> windowConfig.windowPlacement ?: preferredPlacement
                } ?: WindowPlacement.Floating,
                size = size,
            ),
            defaultTitle = defaultTitle,
            titleTransform = titleTransform,
        )
    }

    LaunchedEffect(Unit) {
        snapshotFlow { state.placement }.drop(1).collect { windowConfig.windowPlacement = it }
    }

    return state
}

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
    private val defaultTitle: String? = null,
    private val titleTransform: ((String) -> String)? = null,
) : WindowState by windowState {

    private var window: ComposeWindow? = null
    private val titles = ArrayDeque<Pair<String, String>>()

    internal fun setCompositionTitle(id: String, titleText: String) {

        titles.addLast(id to titleText)

        updateTitle()
    }

    internal fun removeCompositionTitle(id: String) {

        val iterator = titles.listIterator()

        while (iterator.hasPrevious()) {
            if (iterator.previous().first == id) {
                iterator.remove()
                break
            }
        }

        updateTitle()
    }

    internal fun setWindow(window: ComposeWindow) {

        // Window already cached
        if (this.window != null) return

        // Cache window
        this.window = window

        updateTitle()
    }

    private fun updateTitle() {
        val newTitle = titles.lastOrNull()?.second ?: defaultTitle
        if (newTitle != null) {
            window?.title = titleTransform?.invoke(newTitle) ?: newTitle
        }
    }

    fun toFront() {
        window?.toFront()
    }

    fun toBack() {
        window?.toBack()
    }
}

val LocalAppWindowState = staticCompositionLocalOf<AppWindowState> { error("AppWindowState not set") }

@Stable
class AppWindowOwner {

    private val children = mutableListOf<AppWindowState>()

    @Composable
    fun Window(
        content: @Composable () -> Unit,
    ) {

        CompositionLocalProvider(
            LocalAppWindowOwner provides this,
            content = content,
        )
    }

    fun childrenToFront() {
        children.forEach { it.toFront() }
    }

    fun childrenToBack() {
        children.forEach { it.toBack() }
    }

    internal fun registerChild(appWindowState: AppWindowState) {
        children.add(appWindowState)
    }

    internal fun unRegisterChild(appWindowState: AppWindowState) {
        children.remove(appWindowState)
    }
}

val LocalAppWindowOwner = staticCompositionLocalOf<AppWindowOwner?> { null }
