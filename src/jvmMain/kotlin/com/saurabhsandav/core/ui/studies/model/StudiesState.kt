package com.saurabhsandav.core.ui.studies.model

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.ui.common.app.AppWindowsManager
import com.saurabhsandav.core.ui.studies.impl.Study

@Immutable
internal data class StudiesState(
    val studyFactories: List<Study.Factory<*>>,
    val studyWindowsManager: AppWindowsManager<Study.Factory<*>>,
    val eventSink: (StudiesEvent) -> Unit,
)
