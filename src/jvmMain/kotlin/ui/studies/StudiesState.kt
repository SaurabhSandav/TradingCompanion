package ui.studies

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateListOf
import studies.Study

@Immutable
internal data class StudiesState(
    val studies: List<Study>,
)

internal class StudyWindowsManager {

    val windows = mutableStateListOf<StudyWindowState>()

    fun openNewWindow(study: Study) {

        windows += StudyWindowState(
            study = study,
            onCloseRequest = windows::remove,
        )
    }
}

internal class StudyWindowState(
    val study: Study,
    val onCloseRequest: (StudyWindowState) -> Unit,
) {

    fun close() = onCloseRequest(this)
}
