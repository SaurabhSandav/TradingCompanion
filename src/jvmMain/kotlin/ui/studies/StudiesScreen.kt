package ui.studies

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import studies.Study
import ui.common.AppWindow
import ui.common.MultipleWindowManager

@Composable
internal fun StudiesScreen(
    presenter: StudiesPresenter,
) {

    val state by presenter.state.collectAsState()
    val studyWindowsManager = remember { MultipleWindowManager<Study.Factory<*>>() }

    LazyColumn(Modifier.fillMaxSize()) {

        items(items = state.studyFactories) { studyFactory ->

            ListItem(
                modifier = Modifier.clickable { studyWindowsManager.openNewWindow(studyFactory) },
                headlineText = { Text(studyFactory.name) },
            )
        }
    }

    studyWindowsManager.windows.forEach { windowEntry ->

        key(windowEntry) {

            val studyFactory = windowEntry.params

            AppWindow(
                onCloseRequest = { windowEntry.close() },
                state = rememberWindowState(placement = WindowPlacement.Maximized),
                title = studyFactory.name,
            ) {

                val study = remember(studyFactory) { studyFactory.create() }

                study.render()
            }
        }
    }
}
