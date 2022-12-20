package ui.common

import androidx.compose.runtime.*

@Stable
class AppWindowState {

    var title by mutableStateOf("Untitled")
}

val LocalAppWindowState = compositionLocalOf { AppWindowState() }
