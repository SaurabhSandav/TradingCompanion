package com.saurabhsandav.core.ui.studies

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.ui.studies.impl.Study

@Immutable
internal data class StudiesState(
    val studyFactories: List<Study.Factory<*>>,
)
