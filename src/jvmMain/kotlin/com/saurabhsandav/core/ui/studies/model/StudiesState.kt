package com.saurabhsandav.core.ui.studies.model

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.ui.common.app.AppWindowsManager
import com.saurabhsandav.core.ui.studies.impl.Study
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class StudiesState(
    val studyFactories: ImmutableList<Study.Factory<*>>,
    val studyWindowsManager: AppWindowsManager<Study.Factory<*>>,
)
