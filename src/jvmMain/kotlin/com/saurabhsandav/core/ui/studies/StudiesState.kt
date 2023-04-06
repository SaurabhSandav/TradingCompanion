package com.saurabhsandav.core.ui.studies

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.ui.studies.impl.Study
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class StudiesState(
    val studyFactories: ImmutableList<Study.Factory<*>>,
)
