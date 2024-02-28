package com.saurabhsandav.core.ui.studies.model

import com.saurabhsandav.core.ui.common.app.AppWindowsManager
import com.saurabhsandav.core.ui.studies.impl.Study

internal data class StudiesState(
    val studyFactories: List<Study.Factory<*>>,
    val studyWindowsManager: AppWindowsManager<Study.Factory<*>>,
    val eventSink: (StudiesEvent) -> Unit,
)
