package ui.studies

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateListOf
import studies.Study

@Immutable
internal data class StudiesState(
    val studyFactories: List<Study.Factory<*>>,
)

internal class StudyWindowsManager {

    val windows = mutableStateListOf<StudyWindowState>()

    fun openNewWindow(studyFactory: Study.Factory<*>) {

        windows += StudyWindowState(
            studyFactory = studyFactory,
            onCloseRequest = windows::remove,
        )
    }
}

internal class StudyWindowState(
    val studyFactory: Study.Factory<*>,
    val onCloseRequest: (StudyWindowState) -> Unit,
) {

    fun close() = onCloseRequest(this)
}
