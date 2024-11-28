package com.saurabhsandav.core.ui.stats

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.WindowPlacement
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.app.AppWindowsManager
import com.saurabhsandav.core.ui.common.app.WindowTitle
import com.saurabhsandav.core.ui.common.app.rememberAppWindowState
import com.saurabhsandav.core.ui.stats.model.StatsState.StatsCategory
import com.saurabhsandav.core.ui.stats.studies.Study
import com.saurabhsandav.core.ui.stats.ui.LoadedStats

@Composable
internal fun StatsScreen(
    statsCategories: List<StatsCategory>?,
    studyFactories: List<Study.Factory<*>>,
    onOpenStudy: (Study.Factory<*>) -> Unit,
) {

    // Set window title
    WindowTitle("Stats")

    when {
        statsCategories != null -> LoadedStats(
            statsCategories = statsCategories,
            studyFactories = studyFactories,
            onOpenStudy = onOpenStudy,
        )

        else -> Text(
            modifier = Modifier.fillMaxSize().wrapContentSize(),
            text = "No Trades",
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
internal fun StatsScreenWindows(
    studyWindowsManager: AppWindowsManager<Study.Factory<*>>,
) {

    // Study windows
    studyWindowsManager.Windows { window ->

        StudyWindowWindow(
            studyFactory = window.params,
            onCloseRequest = window::close,
        )
    }
}

@Composable
private fun StudyWindowWindow(
    studyFactory: Study.Factory<*>,
    onCloseRequest: () -> Unit,
) {

    AppWindow(
        onCloseRequest = onCloseRequest,
        state = rememberAppWindowState(preferredPlacement = WindowPlacement.Maximized),
        title = studyFactory.name,
    ) {

        val study = remember(studyFactory) { studyFactory.create() }

        study.render()
    }
}
