package com.saurabhsandav.core.ui.studies

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.studies.Study

@Immutable
internal data class StudiesState(
    val studyFactories: List<Study.Factory<*>>,
)
