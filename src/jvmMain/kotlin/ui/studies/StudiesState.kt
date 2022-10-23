package ui.studies

import androidx.compose.runtime.Immutable
import studies.Study

@Immutable
internal data class StudiesState(
    val studyFactories: List<Study.Factory<*>>,
)
