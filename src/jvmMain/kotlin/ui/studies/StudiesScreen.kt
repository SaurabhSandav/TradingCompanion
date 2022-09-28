package ui.studies

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import studies.Study
import ui.common.table.DefaultTableRow
import ui.common.table.LazyTable
import ui.common.table.rows

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

    studyWindowsManager.windows.forEach { windowState ->

        key(windowState) {

            Window(
                onCloseRequest = { windowState.close() },
                state = rememberWindowState(placement = WindowPlacement.Maximized),
                title = windowState.study.name,
            ) {

                val density = LocalDensity.current

                val newDensity = Density(density.density * 0.8F, density.fontScale)

                CompositionLocalProvider(LocalDensity provides newDensity) {
                    StudyWindowContent(windowState.study)
                }
            }
        }
    }
}

@Composable
private fun StudyWindowContent(study: Study) {

    val items by study.provider.data.collectAsState(emptyList())

    LazyTable(
        modifier = Modifier.fillMaxSize(),
        schema = study.provider.schema,
    ) {

        rows(
            items = items,
        ) { item ->

            Column {

                DefaultTableRow(item, schema)

                Divider()
            }
        }
    }
}
