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
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.app.AppWindowOwner
import com.saurabhsandav.core.ui.common.app.WindowTitle
import com.saurabhsandav.core.ui.studies.impl.Study

@Composable
internal fun StudiesScreen(
    presenter: StudiesPresenter,
) {

    val state by presenter.state.collectAsState()
    val studyFactories = remember { mutableStateListOf<Study.Factory<*>>() }
    var bringToFrontStudyFactory by remember { mutableStateOf<Study.Factory<*>?>(null) }

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
                    modifier = Modifier.clickable {
                        if (studyFactory !in studyFactories)
                            studyFactories.add(studyFactory)

                        bringToFrontStudyFactory = studyFactory
                    },
                    headlineText = { Text(studyFactory.name) },
                )
            }
        }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(lazyListState)
        )
    }

    studyFactories.forEach { studyFactory ->

        key(studyFactory) {

            val appWindowOwner = remember { AppWindowOwner() }

            AppWindowOwner(appWindowOwner) {

                StudyWindow(
                    studyFactory = studyFactory,
                    onCloseRequest = { studyFactories.remove(studyFactory) },
                )
            }

            LaunchedEffect(bringToFrontStudyFactory) {
                if (bringToFrontStudyFactory == studyFactory) {
                    appWindowOwner.childrenToFront()
                    bringToFrontStudyFactory = null
                }
            }
        }
    }
}

@Composable
private fun StudyWindow(
    studyFactory: Study.Factory<*>,
    onCloseRequest: () -> Unit,
) {

    AppWindow(
        onCloseRequest = onCloseRequest,
        state = rememberWindowState(placement = WindowPlacement.Maximized),
        title = studyFactory.name,
    ) {

        val study = remember(studyFactory) { studyFactory.create() }

        study.render()
    }
}
