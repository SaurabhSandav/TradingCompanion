package ui.studies

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import ui.common.AppWindow

@Composable
internal fun StudiesScreen(
    presenter: StudiesPresenter,
) {

    val state by presenter.state.collectAsState()
    val studyWindowsManager = remember { StudyWindowsManager() }

    LazyColumn(Modifier.fillMaxSize()) {

        items(items = state.studyFactories) { studyFactory ->

            ListItem(Modifier.clickable { studyWindowsManager.openNewWindow(studyFactory) }) {
                Text(studyFactory.name)
            }
        }
    }

    studyWindowsManager.windows.forEach { windowManager ->

        key(windowManager) {

            AppWindow(
                onCloseRequest = { windowManager.close() },
                state = rememberWindowState(placement = WindowPlacement.Maximized),
                title = windowManager.studyFactory.name,
            ) {

                val study = remember(windowManager.studyFactory) { windowManager.studyFactory.create() }

                study.render()
            }
        }
    }
}
