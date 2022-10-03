package ui.studies

import AppDensityFraction
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState

@Composable
internal fun StudiesScreen(
    presenter: StudiesPresenter,
) {

    val state by presenter.state.collectAsState()
    val studyWindowsManager = remember { StudyWindowsManager() }

    LazyColumn(Modifier.fillMaxSize()) {

        items(items = state.studies) { study ->

            ListItem(Modifier.clickable { studyWindowsManager.openNewWindow(study) }) {
                Text(study.name)
            }
        }
    }

    studyWindowsManager.windows.forEach { windowManager ->

        key(windowManager) {

            Window(
                onCloseRequest = { windowManager.close() },
                state = rememberWindowState(placement = WindowPlacement.Maximized),
                title = windowManager.study.name,
            ) {

                val density = LocalDensity.current

                val newDensity = Density(density.density * AppDensityFraction, density.fontScale)

                CompositionLocalProvider(LocalDensity provides newDensity) {
                    windowManager.study.render()
                }
            }
        }
    }
}
