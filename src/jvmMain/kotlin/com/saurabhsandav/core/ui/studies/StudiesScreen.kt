package com.saurabhsandav.core.ui.studies

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import com.saurabhsandav.core.studies.Study
import com.saurabhsandav.core.ui.common.MultipleWindowManager
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.app.WindowTitle

@Composable
internal fun StudiesScreen(
    presenter: StudiesPresenter,
) {

    val state by presenter.state.collectAsState()
    val studyWindowsManager = remember { MultipleWindowManager<Study.Factory<*>>() }

    // Set window title
    WindowTitle("Studies")

    Box {

        val lazyListState = rememberLazyListState()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = lazyListState,
        ) {

            items(items = state.studyFactories) { studyFactory ->

                ListItem(
                    modifier = Modifier.clickable { studyWindowsManager.openNewWindow(studyFactory) },
                    headlineText = { Text(studyFactory.name) },
                )
            }
        }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(lazyListState)
        )
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
